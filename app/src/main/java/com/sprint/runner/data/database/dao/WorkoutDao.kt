package com.sprint.runner.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sprint.runner.data.database.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface WorkoutDao {
    
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)
    
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)
    
    @Query("SELECT * FROM workouts ORDER BY dateTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE workoutType = :type ORDER BY dateTime DESC")
    fun getWorkoutsByType(type: String): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE dateTime >= :date ORDER BY dateTime DESC")
    fun getWorkoutsFromDate(date: LocalDateTime): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): WorkoutEntity?
    
    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getWorkoutCount(): Int
    
    @Query("SELECT MIN(duration) FROM workouts WHERE isCompleted = 1")
    suspend fun getBestTime(): Long?
    
    @Query("SELECT AVG(duration) FROM workouts WHERE isCompleted = 1")
    suspend fun getAverageTime(): Double?
    
    @Query("DELETE FROM workouts")
    suspend fun clearAllWorkouts()
}