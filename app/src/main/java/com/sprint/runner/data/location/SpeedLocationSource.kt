package com.sprint.runner.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
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

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val speed = if (loc.hasSpeed()) loc.speed else 0f
                val accuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loc.hasSpeedAccuracy()) {
                    loc.speedAccuracyMetersPerSecond
                } else 0f
                val timeMs = loc.elapsedRealtimeNanos / 1_000_000L
                trySend(DistanceSample(speed, accuracy, timeMs))
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
