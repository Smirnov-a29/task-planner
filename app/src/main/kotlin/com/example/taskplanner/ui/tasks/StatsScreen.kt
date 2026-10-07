package com.example.taskplanner.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskplanner.data.model.CountItem
import com.example.taskplanner.data.model.StatsResponse
import com.example.taskplanner.ui.common.ErrorPlaceholder

/** Экран статистики по задачам. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    title: String = "Моя статистика",
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> ErrorPlaceholder(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = viewModel::load
                )

                state.stats != null -> StatsContent(state.stats!!)

                state.isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Содержимое статистики, используется и на отдельном экране, и во вкладке администратора. */
@Composable
fun StatsContent(stats: StatsResponse, modifier: Modifier = Modifier) {
    val progress = if (stats.total == 0) 0f else stats.done.toFloat() / stats.total

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Всего задач", stats.total, Modifier.weight(1f))
            StatTile("Просрочено", stats.overdue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Новые", stats.newCount, Modifier.weight(1f))
            StatTile("В работе", stats.inProgress, Modifier.weight(1f))
            StatTile("Выполнены", stats.done, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text("Доля выполненных задач", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${(progress * 100).toInt()} %",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        Text("Распределение по приоритетам", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        stats.byPriority.forEach { item -> CountRow(item, stats.total) }

        Spacer(Modifier.height(20.dp))
        Text("Распределение по категориям", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (stats.byCategory.isEmpty()) {
            Text("Данных пока нет", style = MaterialTheme.typography.bodyMedium)
        } else {
            stats.byCategory.forEach { item -> CountRow(item, stats.total) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatTile(title: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountRow(item: CountItem, total: Int) {
    val share = if (total == 0) 0f else item.count.toFloat() / total
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(item.count.toString(), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { share },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}
