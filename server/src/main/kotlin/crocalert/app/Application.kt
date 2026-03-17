package crocalert.app

import crocalert.server.FirebaseInit
import crocalert.server.routes.alertRoutes
import crocalert.server.routes.cameraRoutes
import crocalert.server.routes.captureRoutes
import crocalert.server.service.AlertService
import crocalert.server.service.CameraService
import crocalert.server.service.CaptureService
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
    configureRouting(
        alertService  = AlertService(),
        captureService = CaptureService(),
        cameraService  = CameraService()
    )
}

// API key guard via CROC_API_KEY env var. Blank = dev mode (no auth). GET / is always open.
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
            encodeDefaults = true
            explicitNulls = false
        })
    }
}

fun Application.configureRouting(
    alertService: AlertService = AlertService(),
    captureService: CaptureService = CaptureService(),
    cameraService: CameraService = CameraService()
) {
    routing {
        get("/") {
            call.respondText("Server running")
        }
        alertRoutes(alertService)
        captureRoutes(captureService)
        cameraRoutes(cameraService)
    }
}
