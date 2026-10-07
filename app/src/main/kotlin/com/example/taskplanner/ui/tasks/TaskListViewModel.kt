package com.example.taskplanner.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.data.model.TaskHistoryEntry
import com.example.taskplanner.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: List<String> = emptyList(),
    val historyVisible: Boolean = false,
    val selectedTask: Task? = null,
    val selectedTaskHistory: List<TaskHistoryEntry> = emptyList(),
    val message: String? = null
)

/**
 * ViewModel списка задач. Используется как на экране пользователя, так и на вкладке
 * «Все задачи» панели администратора (параметр allTasks).
 */
class TaskListViewModel(
    private val repository: TaskRepository,
    private val dataStore: DataStoreManager,
    private val handle: SavedStateHandle,
    private val allTasks: Boolean = false
) : ViewModel() {

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val DEBOUNCE_MS = 500L
    }

    private val _state = MutableStateFlow(
        TaskListUiState(query = handle.get<String>(KEY_QUERY).orEmpty())
    )
    val state: StateFlow<TaskListUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCategories()
        loadTasks()
        viewModelScope.launch {
            dataStore.searchHistoryFlow.collect { list ->
                _state.update { it.copy(history = list) }
            }
        }
    }

    // ---- поиск ----

    fun onQueryChange(value: String) {
        handle[KEY_QUERY] = value                       // сохраняется при повороте экрана
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)                          // debounce перед запросом
            loadTasks()
        }
    }

    fun clearQuery() {
        handle[KEY_QUERY] = ""
        _state.update { it.copy(query = "") }
        searchJob?.cancel()
        loadTasks()
    }

    fun onSearchFocusChanged(focused: Boolean) {
        _state.update { it.copy(historyVisible = focused && it.history.isNotEmpty()) }
    }

    fun hideHistory() = _state.update { it.copy(historyVisible = false) }

    fun onHistoryItemClick(query: String) {
        handle[KEY_QUERY] = query
        _state.update { it.copy(query = query, historyVisible = false) }
        loadTasks()
    }

    fun clearHistory() {
        viewModelScope.launch {
            dataStore.clearSearchHistory()
            _state.update { it.copy(historyVisible = false) }
        }
    }

    private fun rememberQuery() {
        val q = _state.value.query
        if (q.isNotBlank()) viewModelScope.launch { dataStore.addSearchQuery(q) }
    }

    // ---- фильтры ----

    fun onCategorySelected(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        loadTasks()
    }

    fun onStatusSelected(status: String?) {
        _state.update { it.copy(selectedStatus = status) }
        loadTasks()
    }

    // ---- загрузка данных ----

    fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            val result = if (allTasks) {
                repository.getAllTasks(s.query, s.selectedCategory, s.selectedStatus)
            } else {
                repository.getTasks(s.query, s.selectedCategory, s.selectedStatus)
            }
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, tasks = result.data, error = null) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _state.update { it.copy(categories = result.data) }
                else -> {}
            }
        }
    }

    // ---- работа с выбранной задачей ----

    fun openTask(task: Task) {
        rememberQuery()                                  // запрос попадает в историю поиска
        _state.update { it.copy(selectedTask = task, selectedTaskHistory = emptyList(), historyVisible = false) }
        viewModelScope.launch {
            when (val result = repository.getHistory(task.id)) {
                is ApiResult.Success -> _state.update { it.copy(selectedTaskHistory = result.data) }
                else -> {}
            }
        }
    }

    fun closeTask() = _state.update { it.copy(selectedTask = null, selectedTaskHistory = emptyList()) }

    fun changeStatus(taskId: String, status: String) {
        viewModelScope.launch {
            when (val result = repository.updateStatus(taskId, status)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selectedTask = result.data, message = "Статус задачи обновлён") }
                    loadTasks()
                    when (val h = repository.getHistory(taskId)) {
                        is ApiResult.Success -> _state.update { it.copy(selectedTaskHistory = h.data) }
                        else -> {}
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteTask(taskId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selectedTask = null, message = "Задача удалена") }
                    loadTasks()
                }
                is ApiResult.Error -> _state.update { it.copy(message = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
