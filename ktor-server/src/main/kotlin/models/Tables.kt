package models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * Таблица пользователей приложения «Планировщик задач».
 */
object Users : Table("users") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val login = varchar("login", 255).uniqueIndex()
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 16)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Справочник категорий задач (Работа, Учёба, Дом и т. д.).
 */
object Categories : Table("categories") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Основная таблица задач.
 */
object Tasks : Table("tasks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)
    val categoryId = varchar("category_id", 36)
        .references(Categories.id, onDelete = ReferenceOption.RESTRICT)
    val title = varchar("title", 500)
    val description = text("description")
    val priority = varchar("priority", 16)
    val status = varchar("status", 16)
    val dueDate = varchar("due_date", 32)
    val createdAt = varchar("created_at", 32)
    val updatedAt = varchar("updated_at", 32)

    override val primaryKey = PrimaryKey(id)
}

/**
 * История изменения статусов задачи. Запись создаётся сервером автоматически.
 */
object TaskHistory : Table("task_history") {
    val id = varchar("id", 36)
    val taskId = varchar("task_id", 36)
        .references(Tasks.id, onDelete = ReferenceOption.CASCADE)
    val oldStatus = varchar("old_status", 16)
    val newStatus = varchar("new_status", 16)
    val changedAt = varchar("changed_at", 32)
    val changedBy = varchar("changed_by", 255)

    override val primaryKey = PrimaryKey(id)
}
