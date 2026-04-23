package crocalert.app

import crocalert.server.FirebaseInit
import crocalert.server.routes.alertRoutes
import crocalert.server.routes.cameraRoutes
import crocalert.server.routes.captureRoutes
import crocalert.server.routes.siteRoutes
import crocalert.server.service.AlertService
import crocalert.server.service.AlertServicePort
import crocalert.server.service.CameraService
import crocalert.server.service.CameraServicePort
import crocalert.server.service.CaptureService
import crocalert.server.service.CaptureServicePort
import crocalert.server.service.SiteService
import crocalert.server.service.SiteServicePort
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
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

fun Application.module(initFirebase: Boolean = true) {
    if (initFirebase) FirebaseInit.init()
    configureSerialization()
    configureErrorHandling()
    configureAuth()
    configureRouting(
        alertService   = AlertService(),
        cameraService  = CameraService(),
        captureService = CaptureService(),
        siteService    = SiteService()
    )
}

fun Application.configureAuth(apiKey: String = System.getenv("CROC_API_KEY").orEmpty()) {
    if (apiKey.isBlank()) return   // dev mode — skip

    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.path() == "/") return@intercept
        val provided = call.request.header("X-API-Key")
        if (provided != apiKey) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or missing X-API-Key header")
            finish()
        }
    }
}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "Bad request"))
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "Invalid request parameter"))
            )
        }
        exception<UnsupportedMediaTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "Bad request"))
            )
        }
        status(HttpStatusCode.UnsupportedMediaType) { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid or missing request body")
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Unexpected server error"))
            )
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
    alertService: AlertServicePort = AlertService(),
    cameraService: CameraServicePort = CameraService(),
    captureService: CaptureServicePort = CaptureService(),
    siteService: SiteServicePort = SiteService()
) {
    routing {
        get("/") {
            call.respondText("Server running")
        }
        alertRoutes(alertService)
        cameraRoutes(cameraService)
        captureRoutes(captureService)
        siteRoutes(siteService)
    }
}
