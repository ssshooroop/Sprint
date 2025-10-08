package com.sprint.runner.presentation.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.model.WorkoutType
import com.sprint.runner.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

sealed class TimerState {
    object Ready : TimerState()
    data class Preparation(val remainingTime: Long) : TimerState()
    data class Sprinting(val elapsedTime: Long, val currentCycle: Int, val totalCycles: Int) : TimerState()
    data class Resting(val remainingTime: Long, val currentCycle: Int, val totalCycles: Int) : TimerState()
    object Paused : TimerState()
    data class Completed(val totalTime: Long) : TimerState()
}

sealed class WorkoutMode {
    object TimeBased : WorkoutMode()
    data class DistanceBased(val distance: Int) : WorkoutMode()
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Ready)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _workoutMode = MutableStateFlow<WorkoutMode>(WorkoutMode.TimeBased)
    val workoutMode: StateFlow<WorkoutMode> = _workoutMode.asStateFlow()
    
    private val _settings = MutableStateFlow(
        TimerSettings(
            prepTime = 10000L, // 10 seconds
            sprintTime = 30000L, // 30 seconds
            restTime = 60000L, // 60 seconds
            totalCycles = 3
        )
    )
    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()
    
    private var timerJob: Job? = null
    private var currentWorkout: Workout? = null
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var currentCycle: Int = 0
    
    fun setWorkoutMode(mode: WorkoutMode) {
        _workoutMode.value = mode
    }
    
    fun updateSettings(newSettings: TimerSettings) {
        _settings.value = newSettings
    }
    
    fun startWorkout() {
        if (_timerState.value is TimerState.Paused) {
            resumeWorkout()
            return
        }
        
        timerJob?.cancel()
        
        // Create new workout
        currentWorkout = Workout(
            dateTime = LocalDateTime.now(),
            workoutType = when (_workoutMode.value) {
                is WorkoutMode.TimeBased -> WorkoutType.TimeBased
                is WorkoutMode.DistanceBased -> WorkoutType.DistanceBased(
                    (_workoutMode.value as WorkoutMode.DistanceBased).distance
                )
            },
            duration = 0L,
            cycles = _settings.value.totalCycles,
            prepTime = _settings.value.prepTime,
            sprintTime = _settings.value.sprintTime,
            restTime = _settings.value.restTime,
            isCompleted = false
        )
        
        startPreparation()
    }
    
    private fun startPreparation() {
        _timerState.value = TimerState.Preparation(_settings.value.prepTime)
        startTime = System.currentTimeMillis()
        currentCycle = 0
        
        timerJob = viewModelScope.launch {
            var remainingTime = _settings.value.prepTime
            while (remainingTime > 0) {
                delay(100)
                remainingTime -= 100
                _timerState.value = TimerState.Preparation(remainingTime)
            }
            startSprinting()
        }
    }
    
    private fun startSprinting() {
        currentCycle++
        _timerState.value = TimerState.Sprinting(0L, currentCycle, _settings.value.totalCycles)
        startTime = System.currentTimeMillis()
        
        timerJob = viewModelScope.launch {
            var elapsedTime = 0L
            val targetTime = when (_workoutMode.value) {
                is WorkoutMode.TimeBased -> _settings.value.sprintTime
                is WorkoutMode.DistanceBased -> Long.MAX_VALUE // Run until stopped
            }
            
            while (elapsedTime < targetTime) {
                delay(100)
                elapsedTime = System.currentTimeMillis() - startTime
                _timerState.value = TimerState.Sprinting(
                    elapsedTime = elapsedTime,
                    currentCycle = currentCycle,
                    totalCycles = _settings.value.totalCycles
                )
            }
            
            if (currentCycle < _settings.value.totalCycles) {
                startResting()
            } else {
                completeWorkout()
            }
        }
    }
    
    private fun startResting() {
        _timerState.value = TimerState.Resting(_settings.value.restTime, currentCycle, _settings.value.totalCycles)
        startTime = System.currentTimeMillis()
        
        timerJob = viewModelScope.launch {
            var remainingTime = _settings.value.restTime
            while (remainingTime > 0) {
                delay(100)
                remainingTime -= 100
                _timerState.value = TimerState.Resting(remainingTime, currentCycle, _settings.value.totalCycles)
            }
            startSprinting() // Next cycle
        }
    }
    
    fun pauseWorkout() {
        timerJob?.cancel()
        pausedTime = when (val state = _timerState.value) {
            is TimerState.Preparation -> state.remainingTime
            is TimerState.Sprinting -> state.elapsedTime
            is TimerState.Resting -> state.remainingTime
            else -> 0L
        }
        _timerState.value = TimerState.Paused
    }
    
    private fun resumeWorkout() {
        when (val state = _timerState.value) {
            is TimerState.Paused -> {
                // Resume based on previous state
                timerJob = viewModelScope.launch {
                    // Implementation for resuming from paused state
                }
            }
            else -> {}
        }
    }
    
    fun stopWorkout() {
        timerJob?.cancel()
        
        val finalTime = when (val state = _timerState.value) {
            is TimerState.Sprinting -> state.elapsedTime
            else -> 0L
        }
        
        currentWorkout?.let { workout ->
            val completedWorkout = workout.copy(
                duration = finalTime,
                isCompleted = true
            )
            
            viewModelScope.launch {
                workoutRepository.saveWorkout(completedWorkout)
            }
        }
        
        _timerState.value = TimerState.Completed(finalTime)
    }
    
    fun completeWorkout() {
        timerJob?.cancel()
        
        currentWorkout?.let { workout ->
            val completedWorkout = workout.copy(
                duration = _settings.value.sprintTime,
                isCompleted = true
            )
            
            viewModelScope.launch {
                workoutRepository.saveWorkout(completedWorkout)
            }
        }
        
        _timerState.value = TimerState.Completed(_settings.value.sprintTime)
    }
    
    fun resetWorkout() {
        timerJob?.cancel()
        _timerState.value = TimerState.Ready
        currentWorkout = null
        startTime = 0L
        pausedTime = 0L
        currentCycle = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class TimerSettings(
    val prepTime: Long,
    val sprintTime: Long,
    val restTime: Long,
    val totalCycles: Int
)