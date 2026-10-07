package com.example.taskplanner.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.TaskDictionary
import com.example.taskplanner.data.model.TaskRequest
import com.example.taskplanner.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "MEDIUM",
    val status: String = "NEW",
    val dueDate: String = "",            // в формате дд.мм.гггг
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
    val titleError: String? = null,
    val categoryError: String? = null,
    val dueDateError: String? = null
) {
    val isEditMode: Boolean get() = taskId != null
}

class TaskEditViewModel(
    private val repository: TaskRepository,
    handle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditUiState(taskId = handle.get<String>("taskId")))
    val state: StateFlow<TaskEditUiState> = _state.asStateFlow()

    init {
        loadCategories()
        _state.value.taskId?.let { loadTask(it) }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onCategoryChange(v: String) = _state.update { it.copy(category = v, categoryError = null) }
    fun onPriorityChange(v: String) = _state.update { it.copy(priority = v) }
    fun onStatusChange(v: String) = _state.update { it.copy(status = v) }
    fun onDueDateChange(v: String) = _state.update { it.copy(dueDate = v, dueDateError = null) }
    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _state.update { current ->
                    val categories = result.data
                    current.copy(
                        categories = categories,
                        category = current.category.ifBlank { categories.firstOrNull()?.name.orEmpty() }
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(errorMessage = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    private fun loadTask(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getTask(id)) {
                is ApiResult.Success -> {
                    val task = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = task.title,
                            description = task.description,
                            category = task.category,
                            priority = task.priority,
                            status = task.status,
                            dueDate = if (task.dueDate.isBlank()) "" else TaskDictionary.formatDate(task.dueDate)
                        )
                    }
                }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    /** Проверка обязательных полей и формата даты. Возвращает срок в формате сервера. */
    private fun validate(): String? {
        val s = _state.value
        val titleError = if (s.title.isBlank()) "Введите название задачи" else null
        val categoryError = if (s.category.isBlank()) "Выберите категорию" else null

        var isoDate = ""
        var dueDateError: String? = null
        if (s.dueDate.isNotBlank()) {
            val parsed = TaskDictionary.parseRuDate(s.dueDate.trim())
            if (parsed == null) dueDateError = "Формат даты: дд.мм.гггг" else isoDate = parsed
        }

        _state.update {
            it.copy(titleError = titleError, categoryError = categoryError, dueDateError = dueDateError)
        }
        return if (titleError == null && categoryError == null && dueDateError == null) isoDate else null
    }

    fun save() {
        val isoDate = validate() ?: return
        val s = _state.value
        val request = TaskRequest(
            title = s.title.trim(),
            description = s.description.trim(),
            category = s.category,
            priority = s.priority,
            status = s.status,
            dueDate = isoDate
        )

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (s.taskId == null) {
                repository.createTask(request)
            } else {
                repository.updateTask(s.taskId, request)
            }
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(isLoading = false, saved = true) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }
}
