package com.sprint.runner.domain.model

import java.time.LocalDateTime

sealed class WorkoutType {
    object TimeBased : WorkoutType()
    data class DistanceBased(val distance: Int) : WorkoutType()
}

data class Workout(
    val id: Long = 0,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val workoutType: WorkoutType,
    val duration: Long, // in milliseconds
    val cycles: Int = 1,
    val prepTime: Long = 10000, // in milliseconds
    val sprintTime: Long = 30000, // in milliseconds
    val restTime: Long = 60000, // in milliseconds
    val isCompleted: Boolean = true
)

data class WorkoutStats(
    val totalWorkouts: Int,
    val bestTime: Long,
    val averageTime: Long,
    val totalDistance: Int,
    val improvementPercentage: Int
)