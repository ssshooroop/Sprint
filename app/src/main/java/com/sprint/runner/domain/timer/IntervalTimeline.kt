package com.sprint.runner.domain.timer

/**
 * Pure, platform-independent interval-timer logic.
 *
 * The whole workout is modelled as a fixed timeline of [Segment]s laid out on a
 * single time axis that starts at 0 when the user presses Start. Everything the
 * UI needs (current phase, remaining time, progress) is *derived* from a single
 * `elapsed` value — we never decrement counters, so there is no accumulating
 * drift and no dependency on how often we tick.
 *
 * The Android side feeds `elapsed = SystemClock.elapsedRealtime() - sessionStart`
 * into [snapshotAt]. The same logic is mirrored 1:1 in the JS web preview.
 *
 * Keeping this class free of any Android import is deliberate: it is unit-test
 * friendly and can be reused later on Apple Watch (Kotlin Multiplatform).
 */

/** High-level phase the runner is in at a given moment. */
enum class Phase { IDLE, PREP, WORK, REST, DONE }

/**
 * A discrete feedback event fired exactly once when its moment is crossed.
 * The platform layer maps these to vibration patterns and sounds.
 */
enum class Cue {
    /** A 3-2-1 countdown beep in the last seconds before a work round. */
    COUNTDOWN_TICK,

    /** The "start gun" — the instant a work round begins. */
    GO,

    /** A work round just ended (handing off to rest). */
    WORK_END,

    /** The whole workout is finished. */
    FINISH
}

/**
 * User-configurable interval plan, shared by both modes.
 *
 * Time mode uses [prepMs]/[workMs]/[restMs]/[rounds]. Distance mode uses
 * [prepMs]/[distanceM]/[restMs]/[rounds] — the work phase ends at [distanceM]
 * metres instead of after [workMs]. Durations are in milliseconds.
 */
data class IntervalConfig(
    val prepMs: Long = 5_000,
    val workMs: Long = 30_000,
    val restMs: Long = 30_000,
    val rounds: Int = 6,
    val distanceM: Int = 200
) {
    init {
        require(rounds >= 1) { "rounds must be >= 1" }
    }
}

/**
 * An immutable description of "where are we right now", produced by [IntervalTimeline.snapshotAt].
 *
 * @property countUp WORK counts up (show elapsed); PREP/REST count down (show remaining).
 */
data class TimerSnapshot(
    val phase: Phase,
    val round: Int,
    val totalRounds: Int,
    val phaseTotalMs: Long,
    val phaseElapsedMs: Long,
    val phaseRemainingMs: Long,
    val countUp: Boolean,
    val totalElapsedMs: Long,
    val totalDurationMs: Long
) {
    /** Fraction of the current phase completed, 0f..1f (for the progress ring). */
    val phaseProgress: Float
        get() = if (phaseTotalMs <= 0) 0f
        else (phaseElapsedMs.toFloat() / phaseTotalMs).coerceIn(0f, 1f)
}

class IntervalTimeline(val config: IntervalConfig) {

    /** One contiguous block of the timeline. */
    data class Segment(
        val phase: Phase,
        val round: Int,
        val start: Long,
        val duration: Long
    ) {
        val end: Long get() = start + duration
    }

    /** Ordered PREP → (WORK, REST)×rounds, with no trailing REST. */
    val segments: List<Segment> = buildList {
        var t = 0L
        if (config.prepMs > 0) {
            add(Segment(Phase.PREP, 0, t, config.prepMs)); t += config.prepMs
        }
        for (r in 1..config.rounds) {
            add(Segment(Phase.WORK, r, t, config.workMs)); t += config.workMs
            if (r < config.rounds && config.restMs > 0) {
                add(Segment(Phase.REST, r, t, config.restMs)); t += config.restMs
            }
        }
    }

    val totalDurationMs: Long = segments.sumOf { it.duration }

    /** Derive the full UI state for an absolute [elapsed] time on the workout axis. */
    fun snapshotAt(elapsed: Long): TimerSnapshot {
        val e = elapsed.coerceIn(0, totalDurationMs)
        if (e >= totalDurationMs) {
            return TimerSnapshot(
                phase = Phase.DONE,
                round = config.rounds,
                totalRounds = config.rounds,
                phaseTotalMs = 0,
                phaseElapsedMs = 0,
                phaseRemainingMs = 0,
                countUp = false,
                totalElapsedMs = totalDurationMs,
                totalDurationMs = totalDurationMs
            )
        }
        val seg = segments.first { e >= it.start && e < it.end }
        val inSeg = e - seg.start
        return TimerSnapshot(
            phase = seg.phase,
            round = seg.round,
            totalRounds = config.rounds,
            phaseTotalMs = seg.duration,
            phaseElapsedMs = inSeg,
            phaseRemainingMs = seg.duration - inSeg,
            countUp = seg.phase == Phase.WORK,
            totalElapsedMs = e,
            totalDurationMs = totalDurationMs
        )
    }

    /**
     * Cues whose moment falls in the half-open interval (prev, now].
     *
     * Call with prev = -1 on the very first tick so a boundary at t=0 (e.g. a
     * zero-length prep) still fires its GO.
     */
    fun cuesBetween(prev: Long, now: Long): List<Cue> {
        if (now <= prev) return emptyList()
        val cues = mutableListOf<Cue>()
        for (seg in segments) {
            if (seg.phase != Phase.WORK) continue
            // GO at the start of every work round (the prep/rest countdown lands here).
            if (crossed(seg.start, prev, now)) cues += Cue.GO
            // WORK_END when a round finishes — except the final one, which is FINISH.
            if (seg.end < totalDurationMs && crossed(seg.end, prev, now)) cues += Cue.WORK_END
        }
        // 3-2-1 countdown ticks in the last whole seconds of PREP / REST.
        for (seg in segments) {
            if (seg.phase != Phase.PREP && seg.phase != Phase.REST) continue
            for (k in 1..3) {
                val tickAt = seg.end - k * 1000L
                if (tickAt > seg.start && crossed(tickAt, prev, now)) cues += Cue.COUNTDOWN_TICK
            }
        }
        if (crossed(totalDurationMs, prev, now)) cues += Cue.FINISH
        return cues
    }

    private fun crossed(moment: Long, prev: Long, now: Long): Boolean =
        moment > prev && moment <= now
}
