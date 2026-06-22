package com.sprint.runner.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import com.sprint.runner.domain.timer.IntervalConfig

/**
 * Single settings hub shared by all screens. A scrollable list of value chips;
 * tapping one opens a rotary drum [Picker] (scrolls with the watch bezel/crown).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var editing by remember { mutableStateOf<SettingField?>(null) }

    val field = editing
    if (field == null) {
        SettingsList(config = config, onEdit = { editing = it })
    } else {
        FieldPicker(
            field = field,
            currentValue = field.valueOf(config),
            onConfirm = { value ->
                viewModel.setField(field, value)
                editing = null
            },
            onCancel = { editing = null }
        )
    }
}

@Composable
private fun SettingsList(
    config: IntervalConfig,
    onEdit: (SettingField) -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Настройки",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        SettingField.values().forEach { f ->
            item {
                Chip(
                    onClick = { onEdit(f) },
                    label = { Text(f.title) },
                    secondaryLabel = { Text("${f.valueOf(config)}${f.unit}") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FieldPicker(
    field: SettingField,
    currentValue: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val values = field.values
    val initialIndex = values.indexOf(currentValue).coerceAtLeast(0)
    val pickerState = rememberPickerState(
        initialNumberOfOptions = values.size,
        initiallySelectedOption = initialIndex,
        repeatItems = false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = field.title, color = Color(0xFF8B93A1), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Picker(
            state = pickerState,
            contentDescription = field.title,
            modifier = Modifier.size(width = 120.dp, height = 96.dp)
        ) { optionIndex ->
            Text(
                text = "${values[optionIndex]}${field.unit}",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onConfirm(values[pickerState.selectedOption]) }) {
            Text("OK")
        }
    }
}
