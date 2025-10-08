package com.sprint.runner.data.database.converter

import androidx.room.TypeConverter
import com.sprint.runner.domain.model.WorkoutType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, formatter)
        }
    }
    
    @TypeConverter
    fun fromWorkoutType(workoutType: WorkoutType): String {
        return when (workoutType) {
            is WorkoutType.TimeBased -> "TIME_BASED"
            is WorkoutType.DistanceBased -> "DISTANCE_BASED:${workoutType.distance}"
        }
    }
    
    @TypeConverter
    fun toWorkoutType(workoutTypeString: String): WorkoutType {
        return when {
            workoutTypeString == "TIME_BASED" -> WorkoutType.TimeBased
            workoutTypeString.startsWith("DISTANCE_BASED:") -> {
                val distance = workoutTypeString.substringAfter("DISTANCE_BASED:").toIntOrNull() ?: 100
                WorkoutType.DistanceBased(distance)
            }
            else -> WorkoutType.TimeBased // Default fallback
        }
    }
}
