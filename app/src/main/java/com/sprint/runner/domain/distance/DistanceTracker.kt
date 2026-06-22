package com.sprint.runner.domain.distance

/**
 * One speed reading from the location provider.
 *
 * [speedMps] is the chip's Doppler-derived ground speed (much more accurate than
 * differentiating position), [speedAccuracyMps] its 1-sigma estimate (0 if the
 * device doesn't report one), [timeMs] a monotonic timestamp.
 */
data class DistanceSample(
    val speedMps: Float,
    val speedAccuracyMps: Float,
    val timeMs: Long
)

/** Result of feeding one sample: running total plus the step just integrated. */
data class AddResult(
    val total: Double,
    val stepMeters: Double,
    val stepMs: Long
)

/**
 * Measures distance run on short segments by **integrating speed over time**
 * (∑ v·dt), not by summing GPS positions. On a 200 m sprint this is accurate to
 * a few metres where the position method drifts 5–10%. It also handles
 * back-and-forth shuttle runs correctly, since it sums path length.
 *
 * Pure and platform-independent — fed by the Android location provider in
 * production and by a simulated runner in the web preview.
 */
class DistanceTracker(
    /** Speeds below this are treated as 0 (standing/PPG jitter, not running). */
    private val noiseFloorMps: Double = 0.4,
    /** If GPS drops out longer than this, skip integration across the gap. */
    private val maxGapMs: Long = 3_000,
    /** Discard a reading whose speed uncertainty is implausibly large. */
    private val maxSpeedAccuracyMps: Float = 4.0f
) {
    var distanceM: Double = 0.0
        private set

    private var lastSpeedMps: Double = 0.0
    private var lastTimeMs: Long = -1L

    fun reset() {
        distanceM = 0.0
        lastSpeedMps = 0.0
        lastTimeMs = -1L
    }

    /** Integrate one sample and return the new total plus this step. */
    fun add(sample: DistanceSample): AddResult {
        val v = effectiveSpeed(sample)
        if (lastTimeMs < 0) {
            lastTimeMs = sample.timeMs
            lastSpeedMps = v
            return AddResult(distanceM, 0.0, 0L)
        }
        val dtMs = sample.timeMs - lastTimeMs
        lastTimeMs = sample.timeMs
        if (dtMs <= 0 || dtMs > maxGapMs) {
            lastSpeedMps = v
            return AddResult(distanceM, 0.0, 0L)
        }
        // Trapezoidal rule smooths acceleration between ~1 Hz samples.
        val step = (lastSpeedMps + v) / 2.0 * (dtMs / 1000.0)
        distanceM += step
        lastSpeedMps = v
        return AddResult(distanceM, step, dtMs)
    }

    private fun effectiveSpeed(s: DistanceSample): Double {
        val v = s.speedMps
        if (!v.isFinite() || v < noiseFloorMps) return 0.0
        if (s.speedAccuracyMps > 0f && s.speedAccuracyMps > maxSpeedAccuracyMps) return 0.0
        return v.toDouble()
    }

    companion object {
        /**
         * Where, inside the step that just crossed [target], did the crossing
         * happen — as a fraction 0..1 of that step. Lets the caller back-date the
         * latched result so overshoot/reaction never pollutes it.
         */
        fun crossFraction(prevTotal: Double, target: Double, result: AddResult): Double {
            if (result.stepMeters <= 0.0) return 1.0
            return ((target - prevTotal) / result.stepMeters).coerceIn(0.0, 1.0)
        }
    }
}
