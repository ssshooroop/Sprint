package com.sprint.runner.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sprint.runner.domain.distance.DistanceSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams Doppler speed readings from the fused location provider as
 * [DistanceSample]s. Timestamps use the location's monotonic
 * `elapsedRealtimeNanos`, the same clock the workout axis runs on.
 *
 * The caller must hold ACCESS_FINE_LOCATION before collecting.
 */
@Singleton
class SpeedLocationSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun samples(intervalMs: Long = 500L): Flow<DistanceSample> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .setWaitForAccurateLocation(false)
            .build()

        var previous: android.location.Location? = null

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                val speed: Float
                val accuracy: Float
                val source: String
                if (loc.hasSpeed() && loc.speed > 0f) {
                    // Preferred: Doppler-derived ground speed from the chip.
                    speed = loc.speed
                    accuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loc.hasSpeedAccuracy()) {
                        loc.speedAccuracyMetersPerSecond
                    } else 0f
                    source = "doppler"
                } else {
                    // Fallback: derive speed from position delta if the device
                    // doesn't report Doppler speed.
                    val p = previous
                    val dt = if (p != null) (loc.elapsedRealtimeNanos - p.elapsedRealtimeNanos) / 1_000_000_000.0 else 0.0
                    speed = if (p != null && dt > 0) (p.distanceTo(loc) / dt).toFloat() else 0f
                    accuracy = 0f
                    source = "derived"
                }
                previous = loc

                val timeMs = loc.elapsedRealtimeNanos / 1_000_000L
                Log.d(
                    "SprintGPS",
                    "fix src=$source spd=$speed acc=$accuracy hPos=${loc.accuracy} " +
                        "lat=${loc.latitude} lon=${loc.longitude}"
                )
                trySend(DistanceSample(speed, accuracy, timeMs))
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
