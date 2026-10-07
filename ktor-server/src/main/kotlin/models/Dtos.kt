package models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val login: String,
    val email: String,
    val password: String,
    val role: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val role: String,
    val name: String,
    val email: String
)

@Serializable
data class TaskDto(
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
data class CategoryDto(val id: String, val name: String)

@Serializable
data class CategoryRequest(val name: String)

@Serializable
data class TaskHistoryDto(
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

@Serializable
data class SuccessResponse(val success: Boolean = true)

@Serializable
data class ErrorResponse(val error: String)

/** Допустимые значения приоритета и статуса задачи. */
object TaskEnums {
    val PRIORITIES = listOf("LOW", "MEDIUM", "HIGH")
    val STATUSES = listOf("NEW", "IN_PROGRESS", "DONE")
}
