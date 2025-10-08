package com.sprint.runner.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.runner.domain.model.Workout
import com.sprint.runner.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadWorkouts() {
        viewModelScope.launch {
            _isLoading.value = true
            workoutRepository.getAllWorkouts().collect { workouts ->
                _workouts.value = workouts
                _isLoading.value = false
            }
        }
    }
    
    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workoutId)
            // The flow will automatically update the UI
        }
    }
    
    fun loadWorkoutsByType(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Implementation for filtering by type
            _isLoading.value = false
        }
    }
}