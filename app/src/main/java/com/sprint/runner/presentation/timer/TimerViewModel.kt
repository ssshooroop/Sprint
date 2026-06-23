package com.sprint.runner.presentation.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.data.settings.SettingsRepository
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutType
import com.sprint.runner.domain.repository.WorkoutRepository
import com.sprint.runner.domain.timer.Cue
import com.sprint.runner.domain.timer.IntervalConfig
import com.sprint.runner.domain.timer.IntervalTimeline
import com.sprint.runner.domain.timer.Phase
import com.sprint.runner.domain.timer.TimerSnapshot
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

/** Coarse run state the UI switches its controls on. */
enum class RunState { IDLE, RUNNING, PAUSED, DONE }

/** The selected workout mode. Distance is wired later — time mode is the focus now. */
sealed class WorkoutMode {
    object TimeBased : WorkoutMode()
    data class DistanceBased(val distance: Int) : WorkoutMode()
}

/**
 * Drives the time-based interval timer.
 *
 * Precision comes from [IntervalTimeline]: every frame we read the monotonic
 * [SystemClock.elapsedRealtime] clock and ask the timeline "where are we at this
 * elapsed time". Nothing is decremented, so the displayed time never drifts and
 * survives a slow/janky frame. Phase transitions and beeps are derived from the
 * same axis via [IntervalTimeline.cuesBetween].
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _config = MutableStateFlow(IntervalConfig())
    val config: StateFlow<IntervalConfig> = _config.asStateFlow()

    init {
        // Observe the shared settings store. While idle, keep the displayed plan
        // and timeline in sync; a running workout is left untouched until it ends.
        viewModelScope.launch {
            settingsRepository.config.collect { cfg ->
                _config.value = cfg
                if (_runState.value == RunState.IDLE) {
                    timeline = IntervalTimeline(cfg)
                    _snapshot.value = idleSnapshot(cfg)
                }
            }
        }
    }

    private val _runState = MutableStateFlow(RunState.IDLE)
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private val _snapshot = MutableStateFlow(idleSnapshot(IntervalConfig()))
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    private val _workoutMode = MutableStateFlow<WorkoutMode>(WorkoutMode.TimeBased)
    val workoutMode: StateFlow<WorkoutMode> = _workoutMode.asStateFlow()

    /** One-shot feedback events (beeps / vibration). Collected by the screen. */
    private val _cues = MutableSharedFlow<Cue>(extraBufferCapacity = 16)
    val cues: SharedFlow<Cue> = _cues.asSharedFlow()

    private var ticker: Job? = null
    private var timeline = IntervalTimeline(_config.value)

    /** Monotonic instant the workout axis maps its 0 to. Shifts on pause/resume. */
    private var sessionStart = 0L

    /** Elapsed on the workout axis when paused, so we can resume exactly. */
    private var pausedElapsed = 0L

    /** Last elapsed handed to cuesBetween, so each cue fires exactly once. */
    private var lastCueElapsed = -1L

    fun setWorkoutMode(mode: WorkoutMode) {
        _workoutMode.value = mode
    }

    /** Persist a new plan (from the inline steppers). Ignored while running. */
    fun updateConfig(cfg: IntervalConfig) {
        if (_runState.value != RunState.IDLE) return
        _config.value = cfg
        timeline = IntervalTimeline(cfg)
        _snapshot.value = idleSnapshot(cfg)
        viewModelScope.launch { settingsRepository.update(cfg) }
    }

    /** Start / resume depending on current state. */
    fun start() {
        when (_runState.value) {
            RunState.RUNNING -> return
            RunState.PAUSED -> resume()
            RunState.IDLE, RunState.DONE -> beginFresh()
        }
    }

    private fun beginFresh() {
        timeline = IntervalTimeline(_config.value)
        sessionStart = SystemClock.elapsedRealtime()
        pausedElapsed = 0L
        lastCueElapsed = -1L
        _runState.value = RunState.RUNNING
        runTicker()
    }

    private fun resume() {
        // Re-anchor the axis so "now" maps back to where we paused.
        sessionStart = SystemClock.elapsedRealtime() - pausedElapsed
        _runState.value = RunState.RUNNING
        runTicker()
    }

    private fun runTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (_runState.value == RunState.RUNNING) {
                val elapsed = SystemClock.elapsedRealtime() - sessionStart
                emitFrame(elapsed)
                if (elapsed >= timeline.totalDurationMs) {
                    finish()
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private suspend fun emitFrame(elapsed: Long) {
        _snapshot.value = timeline.snapshotAt(elapsed)
        for (cue in timeline.cuesBetween(lastCueElapsed, elapsed)) {
            _cues.emit(cue)
        }
        lastCueElapsed = elapsed
    }

    fun pause() {
        if (_runState.value != RunState.RUNNING) return
        ticker?.cancel()
        pausedElapsed = (SystemClock.elapsedRealtime() - sessionStart)
            .coerceIn(0, timeline.totalDurationMs)
        _runState.value = RunState.PAUSED
    }

    /** Manual stop — records whatever was completed so far. */
    fun stop() {
        ticker?.cancel()
        val elapsed = currentElapsed()
        _snapshot.value = timeline.snapshotAt(elapsed)
        _runState.value = RunState.DONE
        saveWorkout(elapsed)
    }

    private fun finish() {
        _snapshot.value = timeline.snapshotAt(timeline.totalDurationMs)
        _runState.value = RunState.DONE
        saveWorkout(timeline.totalDurationMs)
    }

    fun reset() {
        ticker?.cancel()
        _runState.value = RunState.IDLE
        _snapshot.value = idleSnapshot(_config.value)
        sessionStart = 0L
        pausedElapsed = 0L
        lastCueElapsed = -1L
    }

    private fun currentElapsed(): Long = when (_runState.value) {
        RunState.PAUSED -> pausedElapsed
        else -> (SystemClock.elapsedRealtime() - sessionStart)
            .coerceIn(0, timeline.totalDurationMs)
    }

    private fun saveWorkout(durationMs: Long) {
        val cfg = _config.value
        val workout = Workout(
            dateTime = LocalDateTime.now(),
            workoutType = WorkoutType.TimeBased,
            duration = durationMs,
            cycles = cfg.rounds,
            prepTime = cfg.prepMs,
            sprintTime = cfg.workMs,
            restTime = cfg.restMs,
            isCompleted = durationMs >= timeline.totalDurationMs
        )
        viewModelScope.launch { workoutRepository.saveWorkout(workout) }
    }

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
    }

    companion object {
        /** ~30 fps display refresh; precision is independent of this value. */
        private const val TICK_MS = 33L

        private fun idleSnapshot(cfg: IntervalConfig): TimerSnapshot {
            val total = IntervalTimeline(cfg).totalDurationMs
            return TimerSnapshot(
                phase = Phase.IDLE,
                round = 0,
                totalRounds = cfg.rounds,
                phaseTotalMs = cfg.workMs,
                phaseElapsedMs = 0,
                phaseRemainingMs = cfg.workMs,
                countUp = true,
                totalElapsedMs = 0,
                totalDurationMs = total
            )
        }
    }
}
