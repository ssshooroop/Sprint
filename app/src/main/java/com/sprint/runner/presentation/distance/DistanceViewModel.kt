package com.sprint.runner.presentation.distance

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.data.location.SpeedLocationSource
import com.sprint.runner.data.settings.SettingsRepository
import com.sprint.runner.domain.distance.DistanceTracker
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutType
import com.sprint.runner.domain.repository.WorkoutRepository
import com.sprint.runner.domain.timer.Cue
import com.sprint.runner.domain.timer.IntervalConfig
import com.sprint.runner.domain.timer.Phase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** A single completed sprint rep. */
data class RepResult(val round: Int, val timeMs: Long, val distanceM: Double)

/** Coarse run state (distance mode has no pause — sprints are short). */
enum class DistanceRunState { IDLE, RUNNING, DONE }

/** Everything the distance screen renders. */
data class DistanceSnapshot(
    val phase: Phase = Phase.IDLE,
    val round: Int = 0,
    val totalRounds: Int = 1,
    val targetM: Int = 200,
    val distanceM: Double = 0.0,
    val workElapsedMs: Long = 0L,
    val phaseRemainingMs: Long = 0L,
    val phaseTotalMs: Long = 0L,
    val results: List<RepResult> = emptyList(),
    // Live diagnostics so we can see GPS health on the watch itself.
    val lastSpeedMps: Float = 0f,
    val sampleCount: Int = 0
) {
    /** WORK ring = distance toward target; countdown ring = time remaining. */
    val progress: Float
        get() = when (phase) {
            Phase.WORK -> if (targetM > 0) (distanceM / targetM).toFloat().coerceIn(0f, 1f) else 0f
            Phase.PREP, Phase.REST ->
                if (phaseTotalMs > 0) 1f - (phaseRemainingMs.toFloat() / phaseTotalMs).coerceIn(0f, 1f) else 0f
            else -> 0f
        }
}

/**
 * Distance-mode interval controller.
 *
 * PREP and REST are time-gated (countdowns); WORK is **distance-gated** — it ends
 * when [DistanceTracker] reports the target reached, and the rep time is latched
 * to the interpolated crossing moment so overrun/reaction never pollutes it.
 *
 * GPS is started during PREP so the fix is warm by GO, but speed is only
 * integrated while in WORK.
 */
@HiltViewModel
class DistanceViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val locationSource: SpeedLocationSource,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _config = MutableStateFlow(IntervalConfig())
    val config: StateFlow<IntervalConfig> = _config.asStateFlow()

    private val _runState = MutableStateFlow(DistanceRunState.IDLE)
    val runState: StateFlow<DistanceRunState> = _runState.asStateFlow()

    private val _snapshot = MutableStateFlow(DistanceSnapshot())
    val snapshot: StateFlow<DistanceSnapshot> = _snapshot.asStateFlow()

    private val _cues = MutableSharedFlow<Cue>(extraBufferCapacity = 16)
    val cues: SharedFlow<Cue> = _cues.asSharedFlow()

    private val tracker = DistanceTracker()
    private var ticker: Job? = null
    private var gpsJob: Job? = null

    // Phase bookkeeping (all on the Main dispatcher, so no locking needed).
    private var phase = Phase.IDLE
    private var round = 0
    private var phaseStart = 0L          // elapsedRealtime at phase entry (time phases)
    private var workStart = 0L           // elapsedRealtime at GO
    private var lastCountdownSecond = -1
    private val results = mutableListOf<RepResult>()

    // Diagnostics, surfaced on screen.
    private var lastSpeedMps = 0f
    private var sampleCount = 0

    init {
        viewModelScope.launch {
            settingsRepository.config.collect { cfg ->
                _config.value = cfg
                if (_runState.value == DistanceRunState.IDLE) {
                    _snapshot.value = DistanceSnapshot(
                        targetM = cfg.distanceM,
                        totalRounds = cfg.rounds,
                        phaseTotalMs = cfg.prepMs
                    )
                }
            }
        }
    }

    /** Persist a new plan (from the inline steppers). Ignored while running. */
    fun updateConfig(cfg: IntervalConfig) {
        if (_runState.value != DistanceRunState.IDLE) return
        _config.value = cfg
        _snapshot.value = DistanceSnapshot(
            targetM = cfg.distanceM,
            totalRounds = cfg.rounds,
            phaseTotalMs = cfg.prepMs
        )
        viewModelScope.launch { settingsRepository.update(cfg) }
    }

    fun start() {
        if (_runState.value == DistanceRunState.RUNNING) return
        results.clear()
        round = 0
        sampleCount = 0
        lastSpeedMps = 0f
        tracker.reset()
        _runState.value = DistanceRunState.RUNNING
        enterPrep()
        startGps()
        runTicker()
    }

    fun stop() {
        finishWorkout(saved = round > 0 || results.isNotEmpty())
    }

    fun reset() {
        ticker?.cancel()
        gpsJob?.cancel()
        phase = Phase.IDLE
        _runState.value = DistanceRunState.IDLE
        _snapshot.value = DistanceSnapshot(
            targetM = _config.value.distanceM,
            totalRounds = _config.value.rounds,
            phaseTotalMs = _config.value.prepMs
        )
    }

    // ---- Phase machine -----------------------------------------------------

    private fun enterPrep() {
        phase = Phase.PREP
        phaseStart = SystemClock.elapsedRealtime()
        lastCountdownSecond = -1
    }

    private fun enterWork() {
        phase = Phase.WORK
        round++
        workStart = SystemClock.elapsedRealtime()
        tracker.reset()
        emit(Cue.GO)
    }

    private fun enterRestOrFinish() {
        if (round >= _config.value.rounds) {
            emit(Cue.FINISH)
            finishWorkout(saved = true)
        } else {
            phase = Phase.REST
            phaseStart = SystemClock.elapsedRealtime()
            lastCountdownSecond = -1
        }
    }

    private fun runTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (_runState.value == DistanceRunState.RUNNING) {
                tick()
                delay(33L)
            }
        }
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        when (phase) {
            Phase.PREP -> {
                val remaining = (_config.value.prepMs - (now - phaseStart)).coerceAtLeast(0)
                countdownCues(remaining)
                publish(remaining, _config.value.prepMs)
                if (remaining <= 0L) enterWork()
            }
            Phase.WORK -> publish(0L, 0L) // distance crossing handled in onSample
            Phase.REST -> {
                val remaining = (_config.value.restMs - (now - phaseStart)).coerceAtLeast(0)
                countdownCues(remaining)
                publish(remaining, _config.value.restMs)
                if (remaining <= 0L) enterWork()
            }
            else -> {}
        }
    }

    /** 3-2-1 ticks in the last seconds of a countdown phase. */
    private fun countdownCues(remainingMs: Long) {
        val sec = ((remainingMs + 999) / 1000).toInt()
        if (sec in 1..3 && sec != lastCountdownSecond) {
            lastCountdownSecond = sec
            emit(Cue.COUNTDOWN_TICK)
        }
    }

    // ---- GPS ---------------------------------------------------------------

    private fun startGps() {
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            locationSource.samples().collect { sample ->
                lastSpeedMps = sample.speedMps
                sampleCount++
                Log.d(
                    "SprintDist",
                    "sample phase=$phase spd=${sample.speedMps} acc=${sample.speedAccuracyMps} " +
                        "dist=${"%.1f".format(tracker.distanceM)}"
                )
                if (phase != Phase.WORK) return@collect
                val prev = tracker.distanceM
                val result = tracker.add(sample)
                val target = _config.value.distanceM.toDouble()
                if (prev < target && result.total >= target) {
                    val frac = DistanceTracker.crossFraction(prev, target, result)
                    val crossAbsMs = sample.timeMs - result.stepMs + (frac * result.stepMs).toLong()
                    val repTime = (crossAbsMs - workStart).coerceAtLeast(0)
                    results.add(RepResult(round, repTime, target))
                    emit(if (round >= _config.value.rounds) Cue.FINISH else Cue.WORK_END)
                    enterRestOrFinish()
                }
            }
        }
    }

    // ---- Snapshot / output -------------------------------------------------

    private fun publish(phaseRemaining: Long, phaseTotal: Long) {
        val now = SystemClock.elapsedRealtime()
        _snapshot.value = DistanceSnapshot(
            phase = phase,
            round = round,
            totalRounds = _config.value.rounds,
            targetM = _config.value.distanceM,
            distanceM = if (phase == Phase.WORK) tracker.distanceM else 0.0,
            workElapsedMs = if (phase == Phase.WORK) now - workStart else 0L,
            phaseRemainingMs = phaseRemaining,
            phaseTotalMs = phaseTotal,
            results = results.toList(),
            lastSpeedMps = lastSpeedMps,
            sampleCount = sampleCount
        )
    }

    private fun finishWorkout(saved: Boolean) {
        ticker?.cancel()
        gpsJob?.cancel()
        phase = Phase.DONE
        _runState.value = DistanceRunState.DONE
        _snapshot.value = _snapshot.value.copy(phase = Phase.DONE, results = results.toList())
        if (saved && results.isNotEmpty()) {
            val totalTime = results.sumOf { it.timeMs }
            val cfg = _config.value
            viewModelScope.launch {
                workoutRepository.saveWorkout(
                    Workout(
                        dateTime = LocalDateTime.now(),
                        workoutType = WorkoutType.DistanceBased(cfg.distanceM),
                        duration = totalTime,
                        cycles = results.size,
                        prepTime = cfg.prepMs,
                        sprintTime = 0L,
                        restTime = cfg.restMs,
                        isCompleted = results.size >= cfg.rounds
                    )
                )
            }
        }
    }

    private fun emit(cue: Cue) {
        viewModelScope.launch { _cues.emit(cue) }
    }

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
        gpsJob?.cancel()
    }
}
