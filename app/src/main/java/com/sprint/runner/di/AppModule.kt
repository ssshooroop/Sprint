package com.sprint.runner.di

import android.content.Context
import androidx.room.Room
import com.sprint.runner.data.database.WorkoutDatabase
import com.sprint.runner.data.datasource.LocalDataSource
import com.sprint.runner.data.repository.WorkoutRepositoryImpl
import com.sprint.runner.domain.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideWorkoutDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            WorkoutDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideWorkoutDao(database: WorkoutDatabase) = database.workoutDao()
    
    @Provides
    @Singleton
    fun provideLocalDataSource(workoutDao: WorkoutDatabase): LocalDataSource {
        return LocalDataSource(workoutDao.workoutDao())
    }
    
    @Provides
    @Singleton
    fun provideWorkoutRepository(localDataSource: LocalDataSource): WorkoutRepository {
        return WorkoutRepositoryImpl(localDataSource)
    }
}