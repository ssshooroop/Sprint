package com.sprint.runner.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.data.settings.SettingsRepository
import com.sprint.runner.domain.timer.IntervalConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A single editable setting and the discrete values its drum picker offers. */
enum class SettingField(
    val title: String,
    val unit: String,
    val values: List<Int>
) {
    PREP("Подготовка", "с", (0..15).toList()),
    WORK("Спринт", "с", (5..120 step 5).toList()),
    REST("Отдых", "с", (5..180 step 5).toList()),
    ROUNDS("Раунды", "", (1..20).toList());

    /** Current value of this field (in its display unit) for a given config. */
    fun valueOf(c: IntervalConfig): Int = when (this) {
        PREP -> (c.prepMs / 1000).toInt()
        WORK -> (c.workMs / 1000).toInt()
        REST -> (c.restMs / 1000).toInt()
        ROUNDS -> c.rounds
    }

    /** Apply a picked value back into the config. */
    fun apply(c: IntervalConfig, value: Int): IntervalConfig = when (this) {
        PREP -> c.copy(prepMs = value * 1000L)
        WORK -> c.copy(workMs = value * 1000L)
        REST -> c.copy(restMs = value * 1000L)
        ROUNDS -> c.copy(rounds = value)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val config: StateFlow<IntervalConfig> =
        repository.config.stateIn(viewModelScope, SharingStarted.Eagerly, IntervalConfig())

    /** Persist a new value for one field; everything else stays as is. */
    fun setField(field: SettingField, value: Int) {
        val updated = field.apply(config.value, value)
        viewModelScope.launch { repository.update(updated) }
    }
}
