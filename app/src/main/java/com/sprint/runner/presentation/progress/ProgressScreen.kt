package com.sprint.runner.presentation.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.sprint.runner.domain.model.WorkoutStats

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadStats()
        viewModel.loadAchievements()
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
                    text = "Прогресс",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats Cards
            StatsSection(stats)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Achievements
            AchievementsSection(achievements)
        }
    }
}

@Composable
private fun StatsSection(stats: WorkoutStats) {
    Column {
        Text(
            text = "Статистика",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Лучшее",
                value = formatTime(stats.bestTime),
                color = Color(0xFF1a73e8),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Среднее",
                value = formatTime(stats.averageTime),
                color = Color(0xFF34a853),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Тренировок",
                value = stats.totalWorkouts.toString(),
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Улучшение",
                value = "${stats.improvementPercentage}%",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { /* Show details */ },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    Column {
        Text(
            text = "Достижения",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement)
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    Card(
        onClick = { /* Show achievement details */ },
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 16.sp
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, remainingSeconds)
    } else {
        String.format("%02d", remainingSeconds)
    }
}
