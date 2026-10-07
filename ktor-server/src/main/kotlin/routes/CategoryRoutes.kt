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
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import models.Categories
import models.CategoryDto
import models.CategoryRequest
import models.SuccessResponse
import models.Tasks
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import plugins.ApiException
import java.util.UUID

private fun requireAdmin(principal: JWTPrincipal) {
    if (principal.payload.getClaim("role").asString() != "ADMIN") {
        throw ApiException(HttpStatusCode.Forbidden, "Доступ только для администратора")
    }
}

private fun allCategories(): List<CategoryDto> = transaction {
    Categories.selectAll()
        .map { CategoryDto(id = it[Categories.id], name = it[Categories.name]) }
        .sortedBy { it.name }
}

fun Route.categoryRoutes() {

    authenticate("auth-jwt") {

        // Справочник категорий доступен всем авторизованным пользователям
        get("/categories") {
            call.respond(allCategories())
        }

        // Добавление категории — только администратор
        post("/categories") {
            val principal = call.principal<JWTPrincipal>()!!
            requireAdmin(principal)
            val req = call.receive<CategoryRequest>()
            val name = req.name.trim()
            if (name.isBlank()) {
                throw ApiException(HttpStatusCode.BadRequest, "Название категории не может быть пустым")
            }

            val created = transaction {
                if (Categories.selectAll().where { Categories.name eq name }.any()) {
                    throw ApiException(HttpStatusCode.Conflict, "Такая категория уже существует")
                }
                val newId = UUID.randomUUID().toString()
                Categories.insert {
                    it[id] = newId
                    it[Categories.name] = name
                }
                CategoryDto(newId, name)
            }
            call.respond(HttpStatusCode.Created, created)
        }

        // Переименование категории — только администратор
        put("/categories/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            requireAdmin(principal)
            val categoryId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор категории")
            val req = call.receive<CategoryRequest>()
            val name = req.name.trim()
            if (name.isBlank()) {
                throw ApiException(HttpStatusCode.BadRequest, "Название категории не может быть пустым")
            }

            val updated = transaction {
                Categories.selectAll().where { Categories.id eq categoryId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")

                val duplicate = Categories.selectAll()
                    .where { Categories.name eq name }
                    .any { it[Categories.id] != categoryId }
                if (duplicate) {
                    throw ApiException(HttpStatusCode.Conflict, "Такая категория уже существует")
                }

                Categories.update({ Categories.id eq categoryId }) {
                    it[Categories.name] = name
                }
                CategoryDto(categoryId, name)
            }
            call.respond(updated)
        }

        // Удаление категории — только администратор и только если она не используется
        delete("/categories/{id}") {
            val principal = call.principal<JWTPrincipal>()!!
            requireAdmin(principal)
            val categoryId = call.parameters["id"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "Не указан идентификатор категории")

            transaction {
                Categories.selectAll().where { Categories.id eq categoryId }.singleOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")

                val used = Tasks.selectAll().where { Tasks.categoryId eq categoryId }.any()
                if (used) {
                    throw ApiException(
                        HttpStatusCode.Conflict,
                        "Категория используется в задачах и не может быть удалена"
                    )
                }
                Categories.deleteWhere { Categories.id eq categoryId }
            }
            call.respond(SuccessResponse())
        }
    }
}
