package com.sprint.runner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sprint.runner.domain.timer.IntervalConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "sprint_settings")

/**
 * Single source of truth for user settings, shared by every screen.
 *
 * Backed by Preferences DataStore so values survive process death and there is
 * exactly one place that owns them — the timer and (later) the distance screen
 * both observe this same [config] flow.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val store = context.dataStore

    val config: Flow<IntervalConfig> = store.data.map { p ->
        IntervalConfig(
            prepMs = p[KEY_PREP] ?: DEFAULT.prepMs,
            workMs = p[KEY_WORK] ?: DEFAULT.workMs,
            restMs = p[KEY_REST] ?: DEFAULT.restMs,
            rounds = p[KEY_ROUNDS] ?: DEFAULT.rounds,
            distanceM = p[KEY_DISTANCE] ?: DEFAULT.distanceM
        )
    }

    suspend fun update(config: IntervalConfig) {
        store.edit { p ->
            p[KEY_PREP] = config.prepMs
            p[KEY_WORK] = config.workMs
            p[KEY_REST] = config.restMs
            p[KEY_ROUNDS] = config.rounds
            p[KEY_DISTANCE] = config.distanceM
        }
    }

    private companion object {
        val DEFAULT = IntervalConfig()
        val KEY_PREP = longPreferencesKey("prep_ms")
        val KEY_WORK = longPreferencesKey("work_ms")
        val KEY_REST = longPreferencesKey("rest_ms")
        val KEY_ROUNDS = intPreferencesKey("rounds")
        val KEY_DISTANCE = intPreferencesKey("distance_m")
    }
}
