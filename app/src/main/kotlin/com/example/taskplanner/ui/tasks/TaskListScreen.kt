package com.example.taskplanner.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.ui.common.EmptyPlaceholder
import com.example.taskplanner.ui.common.ErrorPlaceholder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Главный экран пользователя — список его задач. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    dataStore: DataStoreManager,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val darkTheme by dataStore.darkThemeFlow.collectAsStateWithLifecycle(initialValue = false)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мои задачи") },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Статистика")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val current = dataStore.darkThemeFlow.first()
                            dataStore.setDarkTheme(!current)
                        }
                    }) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Сменить тему"
                        )
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "Профиль")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTask) {
                Icon(Icons.Filled.Add, contentDescription = "Новая задача")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            TaskSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                onFocusChanged = viewModel::onSearchFocusChanged
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            CategoryChips(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = viewModel::onCategorySelected,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            StatusFilterRow(
                selected = state.selectedStatus,
                onSelect = viewModel::onStatusSelected,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.error != null -> ErrorPlaceholder(
                        message = state.error ?: "Ошибка загрузки",
                        onRetry = viewModel::loadTasks
                    )

                    state.tasks.isEmpty() && !state.isLoading ->
                        EmptyPlaceholder(message = "Задачи не найдены")

                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(state.tasks) { task ->
                            TaskCard(task = task, onClick = { viewModel.openTask(task) })
                        }
                    }
                }

                if (state.historyVisible && state.history.isNotEmpty()) {
                    SearchHistoryCard(
                        history = state.history,
                        onItemClick = viewModel::onHistoryItemClick,
                        onClearHistory = viewModel::clearHistory,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    state.selectedTask?.let { task ->
        TaskBottomSheet(
            task = task,
            history = state.selectedTaskHistory,
            canEdit = true,
            onDismiss = viewModel::closeTask,
            onStatusChange = { status -> viewModel.changeStatus(task.id, status) },
            onEdit = {
                viewModel.closeTask()
                onEditTask(task.id)
            },
            onDelete = { viewModel.deleteTask(task.id) }
        )
    }
}
