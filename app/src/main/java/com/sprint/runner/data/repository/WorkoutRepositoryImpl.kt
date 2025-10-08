package com.sprint.runner.data.repository

import com.sprint.runner.data.datasource.LocalDataSource
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutStats
import com.sprint.runner.domain.model.WorkoutType
import com.sprint.runner.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : WorkoutRepository {
    
    override suspend fun saveWorkout(workout: Workout) {
        localDataSource.saveWorkout(workout)
    }
    
    override suspend fun deleteWorkout(workoutId: Long) {
        localDataSource.deleteWorkout(workoutId)
    }
    
    override suspend fun getWorkoutById(workoutId: Long): Workout? {
        return localDataSource.getWorkoutById(workoutId)
    }
    
    override fun getAllWorkouts(): Flow<List<Workout>> {
        return localDataSource.getAllWorkouts()
    }
    
    override fun getWorkoutsByType(type: WorkoutType): Flow<List<Workout>> {
        return localDataSource.getWorkoutsByType(type)
    }
    
    override fun getWorkoutsFromLastDays(days: Int): Flow<List<Workout>> {
        return localDataSource.getWorkoutsFromLastDays(days)
    }
    
    override suspend fun getWorkoutStats(): WorkoutStats {
        return localDataSource.getWorkoutStats()
    }
    
    override suspend fun clearAllWorkouts() {
        localDataSource.clearAllWorkouts()
    }
}