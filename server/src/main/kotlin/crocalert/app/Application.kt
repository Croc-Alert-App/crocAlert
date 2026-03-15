package crocalert.app

import crocalert.server.FirebaseInit
import crocalert.server.routes.alertRoutes
import crocalert.server.routes.cameraRoutes
import crocalert.server.routes.captureRoutes
import crocalert.server.service.AlertService
import crocalert.server.service.CameraService
import crocalert.server.service.CaptureService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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

    val alertService = AlertService()
    val captureService = CaptureService()
    val cameraService = CameraService()

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    routing {
        get("/") {
            call.respondText("Server running")
        }

        alertRoutes(alertService)
        captureRoutes(captureService)
        cameraRoutes(cameraService)
    }
}