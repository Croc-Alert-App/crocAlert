package crocalert.app

import crocalert.server.FirebaseInit
import crocalert.server.routes.alertRoutes
import crocalert.server.service.AlertService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

const val SERVER_PORT = 8080

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    FirebaseInit.init()
    configureSerialization()
    configureAuth()
    configureRouting(AlertService())
}

/**
 * Simple API key guard. Set CROC_API_KEY env var to enable.
 * When the env var is blank the server runs in dev mode with no auth,
 * so staging/prod deployments must always set the variable.
 * The health-check endpoint (GET /) is excluded from auth.
 */
fun Application.configureAuth() {
    val expectedKey = System.getenv("CROC_API_KEY").orEmpty()
    if (expectedKey.isBlank()) return   // dev mode — skip

    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.path() == "/") return@intercept
        val provided = call.request.header("X-API-Key")
        if (provided != expectedKey) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or missing X-API-Key header")
            finish()
        }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureRouting(service: AlertService = AlertService()) {
    routing {
        get("/") {
            call.respondText("Server running")
        }
        alertRoutes(service)
    }
}