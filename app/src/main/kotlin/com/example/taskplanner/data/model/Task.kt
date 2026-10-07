package com.example.taskplanner.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val status: String,
    val dueDate: String,
    val createdAt: String,
    val updatedAt: String,
    val userId: String,
    val userName: String
)

@Serializable
data class TaskRequest(
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val status: String,
    val dueDate: String
)

@Serializable
data class StatusRequest(val status: String)

@Serializable
data class Category(val id: String, val name: String)

@Serializable
data class CategoryRequest(val name: String)

@Serializable
data class TaskHistoryEntry(
    val id: String,
    val taskId: String,
    val oldStatus: String,
    val newStatus: String,
    val changedAt: String,
    val changedBy: String
)

@Serializable
data class CountItem(val name: String, val count: Int)

@Serializable
data class StatsResponse(
    val total: Int,
    val newCount: Int,
    val inProgress: Int,
    val done: Int,
    val overdue: Int,
    val byPriority: List<CountItem>,
    val byCategory: List<CountItem>
)

/**
 * Справочник значений приоритета и статуса задачи, а также вспомогательные функции
 * перевода служебных кодов в русские названия.
 */
object TaskDictionary {

    val PRIORITY_CODES = listOf("HIGH", "MEDIUM", "LOW")
    val STATUS_CODES = listOf("NEW", "IN_PROGRESS", "DONE")

    fun priorityTitle(code: String): String = when (code) {
        "HIGH" -> "Высокий"
        "MEDIUM" -> "Средний"
        "LOW" -> "Низкий"
        else -> code
    }

    fun statusTitle(code: String): String = when (code) {
        "NEW" -> "Новая"
        "IN_PROGRESS" -> "В работе"
        "DONE" -> "Выполнена"
        else -> code
    }

    private val ISO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val RU: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Преобразование даты из формата сервера (2026-05-12) в привычный вид (12.05.2026). */
    fun formatDate(iso: String): String =
        if (iso.isBlank()) "—"
        else runCatching { LocalDate.parse(iso, ISO).format(RU) }.getOrDefault(iso)

    /** Обратное преобразование даты, введённой пользователем. */
    fun parseRuDate(text: String): String? =
        runCatching { LocalDate.parse(text, RU).format(ISO) }.getOrNull()

    /** Задача просрочена, если срок задан, уже прошёл, а задача не выполнена. */
    fun isOverdue(task: Task): Boolean {
        if (task.dueDate.isBlank() || task.status == "DONE") return false
        return runCatching { LocalDate.parse(task.dueDate, ISO).isBefore(LocalDate.now()) }
            .getOrDefault(false)
    }
}
