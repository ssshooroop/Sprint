package com.sprint.runner.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sprint.runner.domain.model.WorkoutType
import java.time.LocalDateTime

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: LocalDateTime,
    val workoutType: WorkoutType,
    val duration: Long,
    val cycles: Int,
    val prepTime: Long,
    val sprintTime: Long,
    val restTime: Long,
    val isCompleted: Boolean
)
