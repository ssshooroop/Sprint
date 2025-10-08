package com.sprint.runner.domain.repository

import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutStats
import com.sprint.runner.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    
    suspend fun saveWorkout(workout: Workout)
    
    suspend fun deleteWorkout(workoutId: Long)
    
    suspend fun getWorkoutById(workoutId: Long): Workout?
    
    fun getAllWorkouts(): Flow<List<Workout>>
    
    fun getWorkoutsByType(type: WorkoutType): Flow<List<Workout>>
    
    fun getWorkoutsFromLastDays(days: Int): Flow<List<Workout>>
    
    suspend fun getWorkoutStats(): WorkoutStats
    
    suspend fun clearAllWorkouts()
}