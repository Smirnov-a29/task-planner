package routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import models.Categories
import models.CountItem
import models.StatsResponse
import models.StatusRequest
import models.SuccessResponse
import models.TaskDto
import models.TaskEnums
import models.TaskHistory
import models.TaskHistoryDto
import models.TaskRequest
import models.Tasks
import models.Users
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import plugins.ApiException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---------- вспомогательные функции ----------

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Соединение трёх таблиц: задача + её категория + владелец. */
private val tasksJoin = Tasks
    .innerJoin(Categories, { Tasks.categoryId }, { Categories.id })
    .innerJoin(Users, { Tasks.userId }, { Users.id })

private fun rowToTask(row: ResultRow): TaskDto = TaskDto(
    id = row[Tasks.id],
    title = row[Tasks.title],
    description = row[Tasks.description],
    category = row[Categories.name],
    priority = row[Tasks.priority],
    status = row[Tasks.status],
    dueDate = row[Tasks.dueDate],
    createdAt = row[Tasks.createdAt],
    updatedAt = row[Tasks.updatedAt],
    userId = row[Tasks.userId],
    userName = row[Users.name]
)

private fun JWTPrincipal.userId(): String = payload.getClaim("userId").asString()

private fun JWTPrincipal.role(): String = payload.getClaim("role").asString()

private fun JWTPrincipal.userName(): String =
    transaction {
        Users.selectAll().where { Users.id eq userId() }.singleOrNull()?.get(Users.name) ?: "—"
    }

private fun JWTPrincipal.requireAdmin() {
    if (role() != "ADMIN") throw ApiException(HttpStatusCode.Forbidden, "Доступ только для администратора")
}

/** Задача просрочена, если срок задан, уже прошёл, а задача не выполнена. */
private fun isOverdue(task: TaskDto): Boolean {
    if (task.dueDate.isBlank() || task.status == "DONE") return false
    return try {
        LocalDate.parse(task.dueDate, DATE).isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

/** Сортировка: сначала невыполненные, внутри — по возрастанию срока, затем по убыванию приоритета. */
private fun sortTasks(list: List<TaskDto>): List<TaskDto> {
    val priorityWeight = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2)
    return list.sortedWith(
        compareBy<TaskDto> { if (it.status == "DONE") 1 else 0 }
            .thenBy { if (it.dueDate.isBlank()) "9999-12-31" else it.dueDate }
            .thenBy { priorityWeight[it.priority] ?: 3 }
    )
}

/** Общий обработчик выборки задач с фильтрами. */
private fun loadTasks(
    ownerId: String?,
    query: String?,
    category: String?,
    status: String?,
    priority: String?
): List<TaskDto> = transaction {
    var condition: Op<Boolean> = Op.TRUE
    if (ownerId != null) condition = condition and (Tasks.userId eq ownerId)
    if (!category.isNullOrBlank()) condition = condition and (Categories.name eq category)
    if (!status.isNullOrBlank()) condition = condition and (Tasks.status eq status)
    if (!priority.isNullOrBlank()) condition = condition and (Tasks.priority eq priority)

    val rows = tasksJoin.selectAll().where { condition }.map { rowToTask(it) }
    val filtered = if (query.isNullOrBlank()) rows else rows.filter {
        it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }
    sortTasks(filtered)
}

/** Проверка прав на конкретную задачу: владелец или администратор. */
private fun requireAccess(principal: JWTPrincipal, taskOwnerId: String) {
    if (principal.role() != "ADMIN" && principal.userId() != taskOwnerId) {
        throw ApiException(HttpStatusCode.Forbidden, "Задача принадлежит другому пользователю")
    }
}

private fun categoryIdByName(name: String): String = transaction {
    Categories.selectAll().where { Categories.name eq name }.singleOrNull()?.get(Categories.id)
        ?: throw ApiException(HttpStatusCode.BadRequest, "Категория «$name» не найдена")
}

private fun validate(req: TaskRequest) {
    if (req.title.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Название задачи не может быть пустым")
    if (req.category.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Не выбрана категория задачи")
    if (req.priority !in TaskEnums.PRIORITIES) throw ApiException(HttpStatusCode.BadRequest, "Некорректный приоритет")
    if (req.status !in TaskEnums.STATUSES) throw ApiException(HttpStatusCode.BadRequest, "Некорректный статус")
    if (req.dueDate.isNotBlank()) {
        try {
            LocalDate.parse(req.dueDate, DATE)
        } catch (e: Exception) {
            throw ApiException(HttpStatusCode.BadRequest, "Некорректная дата срока выполнения")
        }
    }
}

private fun buildStats(tasks: List<TaskDto>): StatsResponse {
    val priorityTitles = linkedMapOf("HIGH" to "Высокий", "MEDIUM" to "Средний", "LOW" to "Низкий")
    return StatsResponse(
        total = tasks.size,
        newCount = tasks.count { it.status == "NEW" },
        inProgress = tasks.count { it.status == "IN_PROGRESS" },
        done = tasks.count { it.status == "DONE" },
        overdue = tasks.count { isOverdue(it) },
        byPriority = priorityTitles.map { (code, title) ->
            CountItem(title, tasks.count { it.priority == code })
        },
        byCategory = tasks.groupBy { it.category }
            .map { (name, items) -> CountItem(name, items.size) }
            .sortedByDescending { it.count }
    )
}

// ---------- маршруты ----------

fun Route.taskRoutes() {

    authenticate("auth-jwt") {

        // Список задач текущего пользователя
        get("/tasks") {
            val principal = call.principal<JWTPrincipal>()!!
            val tasks = loadTasks(
                ownerId = principal.userId(),
                query = call.request.queryParameters["query"],
                category = call.request.queryParameters["category"],
                status = call.request.queryParameters["status"],
                priority = call.request.queryParameters["priority"]
            )
            call.respond(tasks)
        }

        // Список задач всех пользователей — только для администратора
        get("/all-tasks") {
            val principal = call.principal<JWTPrincipal>()!!
            principal.requireAdmin()
            val tasks = loadTasks(
                ownerId = null,
                query = call.request.queryParameters["query"],
                category = call.request.queryParameters["category"],
                status = call.request.queryParameters["status"],
                priority = call.request.queryParameters["priority"]
            )
            call.respond(tasks)
        }

        // Статистика по задачам текущего пользователя
        get("/stats") {
            val principal = call.principal<JWTPrincipal>()!!
            call.respond(buildStats(loadTasks(principal.userId(), null, null, null, null)))
        }

        // Общая статистика системы — только для администратора
        get("/admin/stats") {
            val principal = call.principal<JWTPrincipal>()!!
            principal.requireAdmin()
            call.respond(buildStats(loadTasks(null, null, null, null, null)))
        }

        // История изменения статусов задачи
        get("/tasks/{id}/history") {
            val principal = call.principal<JWTPrincipal>()!!
            val taskId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор задачи")

            val history = transaction {
                val row = Tasks.selectAll().where { Tasks.id eq taskId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Задача не найдена")
                requireAccess(principal, row[Tasks.userId])

                TaskHistory.selectAll().where { TaskHistory.taskId eq taskId }
                    .map {
                        TaskHistoryDto(
                            id = it[TaskHistory.id],
                            taskId = it[TaskHistory.taskId],
                            oldStatus = it[TaskHistory.oldStatus],
                            newStatus = it[TaskHistory.newStatus],
                            changedAt = it[TaskHistory.changedAt],
                            changedBy = it[TaskHistory.changedBy]
                        )
                    }
                    .sortedBy { it.changedAt }
            }
            call.respond(history)
        }

        // Одна задача
        get("/tasks/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            val taskId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор задачи")

            val task = transaction {
                val row = tasksJoin.selectAll().where { Tasks.id eq taskId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Задача не найдена")
                requireAccess(principal, row[Tasks.userId])
                rowToTask(row)
            }
            call.respond(task)
        }

        // Создание задачи
        post("/tasks") {
            val principal = call.principal<JWTPrincipal>()!!
            val req = call.receive<TaskRequest>()
            validate(req)

            val newTaskId = UUID.randomUUID().toString()
            val moment = DatabaseFactory.now()
            val catId = categoryIdByName(req.category)

            val created = transaction {
                Tasks.insert {
                    it[id] = newTaskId
                    it[userId] = principal.userId()
                    it[categoryId] = catId
                    it[title] = req.title.trim()
                    it[description] = req.description.trim()
                    it[priority] = req.priority
                    it[status] = req.status
                    it[dueDate] = req.dueDate
                    it[createdAt] = moment
                    it[updatedAt] = moment
                }
                rowToTask(tasksJoin.selectAll().where { Tasks.id eq newTaskId }.single())
            }
            call.respond(HttpStatusCode.Created, created)
        }

        // Полное редактирование задачи
        put("/tasks/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            val taskId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор задачи")
            val req = call.receive<TaskRequest>()
            validate(req)

            val catId = categoryIdByName(req.category)
            val moment = DatabaseFactory.now()
            val changedBy = principal.userName()

            val updated = transaction {
                val row = Tasks.selectAll().where { Tasks.id eq taskId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Задача не найдена")
                requireAccess(principal, row[Tasks.userId])
                val previousStatus = row[Tasks.status]

                Tasks.update({ Tasks.id eq taskId }) {
                    it[categoryId] = catId
                    it[title] = req.title.trim()
                    it[description] = req.description.trim()
                    it[priority] = req.priority
                    it[status] = req.status
                    it[dueDate] = req.dueDate
                    it[updatedAt] = moment
                }

                if (previousStatus != req.status) {
                    TaskHistory.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[TaskHistory.taskId] = taskId
                        it[oldStatus] = previousStatus
                        it[newStatus] = req.status
                        it[changedAt] = moment
                        it[TaskHistory.changedBy] = changedBy
                    }
                }
                rowToTask(tasksJoin.selectAll().where { Tasks.id eq taskId }.single())
            }
            call.respond(updated)
        }

        // Быстрая смена статуса задачи
        patch("/tasks/{id}/status") {
            val principal = call.principal<JWTPrincipal>()!!
            val taskId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор задачи")
            val req = call.receive<StatusRequest>()
            if (req.status !in TaskEnums.STATUSES) {
                throw ApiException(HttpStatusCode.BadRequest, "Некорректный статус")
            }

            val moment = DatabaseFactory.now()
            val changedBy = principal.userName()

            val updated = transaction {
                val row = Tasks.selectAll().where { Tasks.id eq taskId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Задача не найдена")
                requireAccess(principal, row[Tasks.userId])
                val previousStatus = row[Tasks.status]

                if (previousStatus == req.status) {
                    throw ApiException(HttpStatusCode.Conflict, "Задача уже находится в этом статусе")
                }

                Tasks.update({ Tasks.id eq taskId }) {
                    it[status] = req.status
                    it[updatedAt] = moment
                }
                TaskHistory.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[TaskHistory.taskId] = taskId
                    it[oldStatus] = previousStatus
                    it[newStatus] = req.status
                    it[changedAt] = moment
                    it[TaskHistory.changedBy] = changedBy
                }
                rowToTask(tasksJoin.selectAll().where { Tasks.id eq taskId }.single())
            }
            call.respond(updated)
        }

        // Удаление задачи вместе с историей изменений
        delete("/tasks/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            val taskId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор задачи")

            transaction {
                val row = Tasks.selectAll().where { Tasks.id eq taskId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Задача не найдена")
                requireAccess(principal, row[Tasks.userId])

                TaskHistory.deleteWhere { TaskHistory.taskId eq taskId }
                Tasks.deleteWhere { Tasks.id eq taskId }
            }
            call.respond(SuccessResponse())
        }
    }
}
