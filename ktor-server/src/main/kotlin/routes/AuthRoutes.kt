package routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import models.AuthResponse
import models.LoginRequest
import models.RegisterRequest
import models.SuccessResponse
import models.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import plugins.ApiException
import plugins.JwtConfig
import java.util.UUID

fun Route.authRoutes() {

    post("/auth/register") {
        val req = call.receive<RegisterRequest>()

        // серверная валидация — дублирует клиентскую
        if (req.name.isBlank() || req.login.length < 3 || req.password.length < 6) {
            throw ApiException(HttpStatusCode.BadRequest, "Некорректные данные регистрации")
        }
        if (!req.email.contains("@") || !req.email.contains(".")) {
            throw ApiException(HttpStatusCode.BadRequest, "Некорректный адрес электронной почты")
        }
        val role = if (req.role == "ADMIN") "ADMIN" else "USER"

        transaction {
            val exists = Users.selectAll().where { Users.login eq req.login }.any()
            if (exists) {
                throw ApiException(HttpStatusCode.Conflict, "Пользователь с таким логином уже существует")
            }
            Users.insert {
                it[id] = UUID.randomUUID().toString()
                it[name] = req.name
                it[login] = req.login
                it[email] = req.email
                it[passwordHash] = BCrypt.hashpw(req.password, BCrypt.gensalt())
                it[Users.role] = role
            }
        }
        call.respond(HttpStatusCode.Created, SuccessResponse())
    }

    post("/auth/login") {
        val req = call.receive<LoginRequest>()

        val response = transaction {
            val row = Users.selectAll().where { Users.login eq req.login }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.Unauthorized, "Неверный логин или пароль")

            if (!BCrypt.checkpw(req.password, row[Users.passwordHash])) {
                throw ApiException(HttpStatusCode.Unauthorized, "Неверный логин или пароль")
            }

            val token = JwtConfig.generateToken(
                userId = row[Users.id],
                login = row[Users.login],
                role = row[Users.role]
            )
            AuthResponse(
                token = token,
                role = row[Users.role],
                name = row[Users.name],
                email = row[Users.email]
            )
        }
        call.respond(response)
    }
}
