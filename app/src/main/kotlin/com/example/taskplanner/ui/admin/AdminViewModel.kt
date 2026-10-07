package com.example.taskplanner.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.StatsResponse
import com.example.taskplanner.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val categories: List<Category> = emptyList(),
    val stats: StatsResponse? = null,
    val newCategoryName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/** ViewModel вкладок «Категории» и «Статистика» панели администратора. */
class AdminViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadCategories()
        loadStats()
    }

    fun onNewCategoryNameChange(v: String) = _state.update { it.copy(newCategoryName = v) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getCategories()) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, categories = result.data) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            when (val result = repository.getAdminStats()) {
                is ApiResult.Success -> _state.update { it.copy(stats = result.data) }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun addCategory() {
        val name = _state.value.newCategoryName.trim()
        if (name.isBlank()) {
            _state.update { it.copy(message = "Введите название категории") }
            return
        }
        viewModelScope.launch {
            when (val result = repository.createCategory(name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(newCategoryName = "", message = "Категория добавлена") }
                    loadCategories()
                }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun renameCategory(id: String, name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(message = "Название не может быть пустым") }
            return
        }
        viewModelScope.launch {
            when (val result = repository.updateCategory(id, name.trim())) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = "Категория переименована") }
                    loadCategories()
                }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            when (val result = repository.deleteCategory(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = "Категория удалена") }
                    loadCategories()
                }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }
}
