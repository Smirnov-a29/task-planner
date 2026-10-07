import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import plugins.configureAuthentication
import plugins.configureCors
import plugins.configureSerialization
import plugins.configureStatusPages
import routes.authRoutes
import routes.categoryRoutes
import routes.taskRoutes

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureCors()
    configureStatusPages()
    configureAuthentication()

    routing {
        authRoutes()
        taskRoutes()
        categoryRoutes()
    }
}
