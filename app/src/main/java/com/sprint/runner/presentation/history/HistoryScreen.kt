package com.sprint.runner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Text
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutType
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val workouts by viewModel.workouts.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadWorkouts()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Назад", color = Color.White, fontSize = 12.sp)
                }
                
                Text(
                    text = "История",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Workouts List
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет тренировок",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(workouts) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onDelete = { viewModel.deleteWorkout(workout.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    onDelete: () -> Unit
) {
    Card(
        onClick = { /* Show details */ },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDuration(workout.duration),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = when (workout.workoutType) {
                            is WorkoutType.TimeBased -> "По времени"
                            is WorkoutType.DistanceBased -> "${workout.workoutType.distance}м"
                        },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    
                    if (workout.cycles > 1) {
                        Text(
                            text = "${workout.cycles} циклов",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = workout.dateTime.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("×", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
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