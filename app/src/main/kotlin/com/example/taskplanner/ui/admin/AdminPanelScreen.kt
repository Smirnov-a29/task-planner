package com.example.taskplanner.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.ui.common.EmptyPlaceholder
import com.example.taskplanner.ui.common.ErrorPlaceholder
import com.example.taskplanner.ui.tasks.CategoryChips
import com.example.taskplanner.ui.tasks.SearchHistoryCard
import com.example.taskplanner.ui.tasks.StatsContent
import com.example.taskplanner.ui.tasks.StatusFilterRow
import com.example.taskplanner.ui.tasks.TaskBottomSheet
import com.example.taskplanner.ui.tasks.TaskCard
import com.example.taskplanner.ui.tasks.TaskListViewModel
import com.example.taskplanner.ui.tasks.TaskSearchBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Панель администратора: все задачи, справочник категорий и общая статистика. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    listViewModel: TaskListViewModel,
    adminViewModel: AdminViewModel,
    dataStore: DataStoreManager,
    onOpenProfile: () -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit
) {
    val listState by listViewModel.state.collectAsStateWithLifecycle()
    val adminState by adminViewModel.state.collectAsStateWithLifecycle()
    val darkTheme by dataStore.darkThemeFlow.collectAsStateWithLifecycle(initialValue = false)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(listState.message) {
        listState.message?.let {
            snackbarHostState.showSnackbar(it)
            listViewModel.consumeMessage()
        }
    }
    LaunchedEffect(adminState.message) {
        adminState.message?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Панель администратора") },
                actions = {
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
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onCreateTask) {
                    Icon(Icons.Filled.Add, contentDescription = "Новая задача")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Все задачи") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Категории") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        adminViewModel.loadStats()
                    },
                    text = { Text("Статистика") }
                )
            }

            when (selectedTab) {
                0 -> AllTasksTab(
                    viewModel = listViewModel,
                    onEditTask = onEditTask,
                    onRequestDelete = { taskToDelete = it }
                )

                1 -> CategoriesTab(viewModel = adminViewModel, categories = adminState.categories)

                else -> {
                    val stats = adminState.stats
                    if (stats == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        StatsContent(stats)
                    }
                }
            }
        }
    }

    listState.selectedTask?.let { task ->
        TaskBottomSheet(
            task = task,
            history = listState.selectedTaskHistory,
            canEdit = true,
            onDismiss = listViewModel::closeTask,
            onStatusChange = { status -> listViewModel.changeStatus(task.id, status) },
            onEdit = {
                listViewModel.closeTask()
                onEditTask(task.id)
            },
            onDelete = { listViewModel.deleteTask(task.id) }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Удаление задачи") },
            text = { Text("Удалить задачу «${task.title}» пользователя ${task.userName}?") },
            confirmButton = {
                TextButton(onClick = {
                    listViewModel.deleteTask(task.id)
                    taskToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

/** Вкладка со списком задач всех пользователей. */
@Composable
private fun AllTasksTab(
    viewModel: TaskListViewModel,
    onEditTask: (String) -> Unit,
    onRequestDelete: (Task) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
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
                        TaskCard(
                            task = task,
                            showOwner = true,
                            showActions = true,
                            onClick = { viewModel.openTask(task) },
                            onEdit = { onEditTask(task.id) },
                            onDelete = { onRequestDelete(task) }
                        )
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

/** Вкладка управления справочником категорий. */
@Composable
private fun CategoriesTab(viewModel: AdminViewModel, categories: List<Category>) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var categoryToRename by remember { mutableStateOf<Category?>(null) }
    var renameValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.newCategoryName,
                onValueChange = viewModel::onNewCategoryNameChange,
                label = { Text("Новая категория") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = viewModel::addCategory) {
                Text("Добавить")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (categories.isEmpty()) {
            EmptyPlaceholder(message = "Справочник категорий пуст")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                categoryToRename = category
                                renameValue = category.name
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Переименовать")
                            }
                            IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    categoryToRename?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text("Переименование категории") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("Название") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameCategory(category.id, renameValue)
                    categoryToRename = null
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) { Text("Отмена") }
            }
        )
    }
}
