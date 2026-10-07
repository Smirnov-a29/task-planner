package com.example.taskplanner.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.data.model.TaskDictionary
import com.example.taskplanner.ui.theme.PriorityHigh
import com.example.taskplanner.ui.theme.PriorityLow
import com.example.taskplanner.ui.theme.PriorityMedium
import com.example.taskplanner.ui.theme.StatusDone
import com.example.taskplanner.ui.theme.StatusInProgress
import com.example.taskplanner.ui.theme.StatusNew

fun priorityColor(code: String): Color = when (code) {
    "HIGH" -> PriorityHigh
    "MEDIUM" -> PriorityMedium
    else -> PriorityLow
}

fun statusColor(code: String): Color = when (code) {
    "NEW" -> StatusNew
    "IN_PROGRESS" -> StatusInProgress
    else -> StatusDone
}

/** Небольшая цветная метка со скруглением. */
@Composable
fun ColoredLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

/** Карточка задачи в списке. */
@Composable
fun TaskCard(
    task: Task,
    showOwner: Boolean = false,
    showActions: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val overdue = TaskDictionary.isOverdue(task)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Цветная полоса слева — приоритет задачи
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (showActions) 132.dp else 116.dp)
                    .background(priorityColor(task.priority))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.status == "DONE") TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColoredLabel(TaskDictionary.statusTitle(task.status), statusColor(task.status))
                    ColoredLabel(TaskDictionary.priorityTitle(task.priority), priorityColor(task.priority))
                    AssistChip(onClick = onClick, label = { Text(task.category) })
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (overdue) "Просрочена — ${TaskDictionary.formatDate(task.dueDate)}"
                        else "Срок: ${TaskDictionary.formatDate(task.dueDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showOwner) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = task.userName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(onClick = onDelete) {
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

/** Кружок-индикатор, используемый на экране статистики. */
@Composable
fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}
