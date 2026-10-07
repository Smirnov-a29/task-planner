package com.example.taskplanner.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.StatsResponse
import com.example.taskplanner.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: StatsResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** ViewModel экрана статистики. Параметр admin выбирает источник данных. */
class StatsViewModel(
    private val repository: TaskRepository,
    private val admin: Boolean = false
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = if (admin) repository.getAdminStats() else repository.getStats()
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, stats = result.data) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }
}
