package com.sprint.runner.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sprint.runner.data.database.converter.Converters
import com.sprint.runner.data.database.dao.WorkoutDao
import com.sprint.runner.data.database.entity.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    
    companion object {
        const val DATABASE_NAME = "sprint_runner_database"
    }
}