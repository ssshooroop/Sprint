package com.sprint.runner.data.datasource

import com.sprint.runner.data.database.dao.WorkoutDao
import com.sprint.runner.data.database.entity.WorkoutEntity
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutStats
import com.sprint.runner.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    
    suspend fun saveWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout.toEntity())
    }
    
    suspend fun deleteWorkout(workoutId: Long) {
        workoutDao.deleteWorkoutById(workoutId)
    }
    
    suspend fun getWorkoutById(workoutId: Long): Workout? {
        return workoutDao.getWorkoutById(workoutId)?.toDomainModel()
    }
    
    fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getWorkoutsByType(type: WorkoutType): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { entities ->
            entities.filter { it.workoutType == type }.map { it.toDomainModel() }
        }
    }
    
    fun getWorkoutsFromLastDays(days: Int): Flow<List<Workout>> {
        val date = LocalDateTime.now().minusDays(days.toLong())
        return workoutDao.getWorkoutsFromDate(date).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getWorkoutStats(): WorkoutStats {
        val totalWorkouts = workoutDao.getWorkoutCount()
        val bestTime = workoutDao.getBestTime() ?: 0L
        val averageTime = workoutDao.getAverageTime()?.toLong() ?: 0L
        
        // Calculate improvement percentage
        val workouts = workoutDao.getAllWorkouts().map { entities ->
            entities.filter { it.isCompleted }.map { it.toDomainModel() }
        }
        
        val improvement = calculateImprovement(workouts)
        
        return WorkoutStats(
            totalWorkouts = totalWorkouts,
            bestTime = bestTime,
            averageTime = averageTime,
            totalDistance = 0, // Will be calculated based on workout type
            improvementPercentage = improvement
        )
    }
    
    suspend fun clearAllWorkouts() {
        workoutDao.clearAllWorkouts()
    }
    
    private fun calculateImprovement(workoutsFlow: Flow<List<Workout>>): Int {
        // Simplified improvement calculation
        return 15 // Placeholder - would need more complex logic
    }
    
    private fun Workout.toEntity(): WorkoutEntity {
        return WorkoutEntity(
            id = id,
            dateTime = dateTime,
            workoutType = workoutType,
            duration = duration,
            cycles = cycles,
            prepTime = prepTime,
            sprintTime = sprintTime,
            restTime = restTime,
            isCompleted = isCompleted
        )
    }
    
    private fun WorkoutEntity.toDomainModel(): Workout {
        return Workout(
            id = id,
            dateTime = dateTime,
            workoutType = workoutType,
            duration = duration,
            cycles = cycles,
            prepTime = prepTime,
            sprintTime = sprintTime,
            restTime = restTime,
            isCompleted = isCompleted
        )
    }
}