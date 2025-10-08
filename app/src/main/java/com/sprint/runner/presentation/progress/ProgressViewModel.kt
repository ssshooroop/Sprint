package com.sprint.runner.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutStats
import com.sprint.runner.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Achievement(
    val id: String,
    val name: String,
    val emoji: String,
    val unlocked: Boolean
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _stats = MutableStateFlow(
        WorkoutStats(
            totalWorkouts = 0,
            bestTime = 0L,
            averageTime = 0L,
            totalDistance = 0,
            improvementPercentage = 0
        )
    )
    val stats: StateFlow<WorkoutStats> = _stats.asStateFlow()
    
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()
    
    fun loadStats() {
        viewModelScope.launch {
            val workoutStats = workoutRepository.getWorkoutStats()
            _stats.value = workoutStats
        }
    }
    
    fun loadAchievements() {
        viewModelScope.launch {
            val workouts = workoutRepository.getAllWorkouts()
            workouts.collect { workoutList ->
                val achievements = calculateAchievements(workoutList)
                _achievements.value = achievements
            }
        }
    }
    
    private fun calculateAchievements(workouts: List<Workout>): List<Achievement> {
        // Simplified achievement calculation
        return listOf(
            Achievement("first_workout", "Первый шаг", "🏃", true),
            Achievement("ten_workouts", "Десятка", "💪", workouts.size >= 10),
            Achievement("fast_sprint", "Скорость", "⚡", true),
            Achievement("improvement", "Прогресс", "📈", true),
            Achievement("marathon", "Марафон", "🏁", workouts.size >= 50),
            Achievement("consistency", "Регулярность", "📅", workouts.size >= 7)
        )
    }
}