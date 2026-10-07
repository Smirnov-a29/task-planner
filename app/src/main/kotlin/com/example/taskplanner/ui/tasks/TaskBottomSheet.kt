package com.example.taskplanner.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.data.model.TaskDictionary
import com.example.taskplanner.data.model.TaskHistoryEntry

/** Нижний лист с полной информацией о задаче и быстрым изменением статуса. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBottomSheet(
    task: Task,
    history: List<TaskHistoryEntry>,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }
    val overdue = TaskDictionary.isOverdue(task)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(text = task.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(task.category) })
                ColoredLabel(TaskDictionary.statusTitle(task.status), statusColor(task.status))
                ColoredLabel(TaskDictionary.priorityTitle(task.priority), priorityColor(task.priority))
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Описание")
            Text(
                text = task.description.ifBlank { "Описание не заполнено" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("Срок выполнения")
            Text(
                text = if (task.dueDate.isBlank()) "Срок не задан"
                else TaskDictionary.formatDate(task.dueDate) + if (overdue) " (просрочена)" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = if (overdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("Сведения")
            Text("Автор: ${task.userName}", style = MaterialTheme.typography.bodyMedium)
            Text("Создана: ${task.createdAt}", style = MaterialTheme.typography.bodyMedium)
            Text("Обновлена: ${task.updatedAt}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
            SectionTitle("Изменение статуса")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskDictionary.STATUS_CODES.forEachIndexed { index, code ->
                    SegmentedButton(
                        selected = task.status == code,
                        onClick = { if (task.status != code) onStatusChange(code) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TaskDictionary.STATUS_CODES.size
                        )
                    ) {
                        Text(TaskDictionary.statusTitle(code))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("История изменений")
            if (history.isEmpty()) {
                Text("Статус задачи ещё не изменялся", style = MaterialTheme.typography.bodyMedium)
            } else {
                history.forEach { entry ->
                    Text(
                        text = "${TaskDictionary.statusTitle(entry.oldStatus)} → " +
                            "${TaskDictionary.statusTitle(entry.newStatus)}, " +
                            "${entry.changedAt} (${entry.changedBy})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (canEdit) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text("Редактировать")
                    }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удаление задачи") },
            text = { Text("Удалить задачу «${task.title}»?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
