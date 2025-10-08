package com.sprint.runner.presentation.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.sprint.runner.presentation.timer.TimerState.Completed
import com.sprint.runner.presentation.timer.TimerState.Paused
import com.sprint.runner.presentation.timer.TimerState.Preparation
import com.sprint.runner.presentation.timer.TimerState.Ready
import com.sprint.runner.presentation.timer.TimerState.Resting
import com.sprint.runner.presentation.timer.TimerState.Sprinting

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProgress: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    val workoutMode by viewModel.workoutMode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Timer Display with Progress Ring
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = getProgress(timerState, settings),
                    modifier = Modifier.fillMaxSize(),
                    startAngle = 270f,
                    indicatorColor = getProgressColor(timerState),
                    trackColor = Color.DarkGray,
                    strokeWidth = 6.dp
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatTime(timerState),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = getPhaseText(timerState),
                        color = getPhaseColor(timerState),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (timerState) {
                    is Ready -> {
                        ControlButton(
                            onClick = { viewModel.startWorkout() },
                            text = "СТАРТ",
                            color = Color(0xFF4CAF50)
                        )
                    }
                    is Paused -> {
                        ControlButton(
                            onClick = { viewModel.startWorkout() },
                            text = "ПРОД",
                            color = Color(0xFF4CAF50)
                        )
                        ControlButton(
                            onClick = { viewModel.resetWorkout() },
                            text = "СБРОС",
                            color = Color(0xFF9E9E9E)
                        )
                    }
                    is Sprinting, is Preparation, is Resting -> {
                        ControlButton(
                            onClick = { viewModel.pauseWorkout() },
                            text = "ПАУЗА",
                            color = Color(0xFFFFC107)
                        )
                        ControlButton(
                            onClick = { viewModel.stopWorkout() },
                            text = "СТОП",
                            color = Color(0xFFF44336)
                        )
                    }
                    is Completed -> {
                        ControlButton(
                            onClick = { viewModel.resetWorkout() },
                            text = "НОВЫЙ",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mode Indicator
            Text(
                text = when (workoutMode) {
                    is WorkoutMode.TimeBased -> "По времени"
                    is WorkoutMode.DistanceBased -> "${(workoutMode as WorkoutMode.DistanceBased).distance}м"
                },
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
    
    // Auto-reset completed workout after delay
    LaunchedEffect(timerState) {
        if (timerState is Completed) {
            kotlinx.coroutines.delay(3000)
            viewModel.resetWorkout()
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    text: String,
    color: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = CircleShape
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(timerState: TimerState): String {
    return when (timerState) {
        is Preparation -> formatMilliseconds(timerState.remainingTime)
        is Sprinting -> formatMilliseconds(timerState.elapsedTime)
        is Resting -> formatMilliseconds(timerState.remainingTime)
        is Completed -> formatMilliseconds(timerState.totalTime)
        else -> "00:00"
    }
}

private fun formatMilliseconds(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val centiseconds = (millis % 1000) / 10
    
    return if (minutes > 0) {
        String.format("%d:%02d.%02d", minutes, remainingSeconds, centiseconds)
    } else {
        String.format("%02d.%02d", remainingSeconds, centiseconds)
    }
}

private fun getProgress(timerState: TimerState, settings: TimerSettings): Float {
    return when (timerState) {
        is Preparation -> timerState.remainingTime.toFloat() / settings.prepTime
        is Sprinting -> {
            if (settings.sprintTime > 0) {
                timerState.elapsedTime.toFloat() / settings.sprintTime
            } else 0f
        }
        is Resting -> timerState.remainingTime.toFloat() / settings.restTime
        else -> 0f
    }.coerceIn(0f, 1f)
}

private fun getProgressColor(timerState: TimerState): Color {
    return when (timerState) {
        is Preparation -> Color(0xFFFF9800)
        is Sprinting -> Color(0xFF4CAF50)
        is Resting -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
}

private fun getPhaseText(timerState: TimerState): String {
    return when (timerState) {
        is Ready -> "Готов"
        is Preparation -> "Подготовка"
        is Sprinting -> "Спринт ${timerState.currentCycle}/${timerState.totalCycles}"
        is Resting -> "Отдых ${timerState.currentCycle}/${timerState.totalCycles}"
        is Paused -> "Пауза"
        is Completed -> "Готово!"
    }
}

private fun getPhaseColor(timerState: TimerState): Color {
    return when (timerState) {
        is Ready -> Color.Gray
        is Preparation -> Color(0xFFFF9800)
        is Sprinting -> Color(0xFF4CAF50)
        is Resting -> Color(0xFF2196F3)
        is Paused -> Color(0xFFFFC107)
        is Completed -> Color(0xFF4CAF50)
    }
}