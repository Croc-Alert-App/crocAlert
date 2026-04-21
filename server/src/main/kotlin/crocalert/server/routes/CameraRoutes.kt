package crocalert.server.routes

import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.service.CameraServicePort
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.LocalDate
import java.time.format.DateTimeParseException

// P3: strict calendar validation — rejects impossible dates like 2026-13-45 or 2026-02-30
private fun String.isValidDate(): Boolean = try {
    LocalDate.parse(this)
    true
} catch (_: DateTimeParseException) {
    false
}

// P4: blank or whitespace cameraId produces confusing 404s — reject early
private fun String.isValidId(): Boolean = isNotBlank()

// P2: uniform structured error response for service exceptions
private data class ErrorResponse(val error: String)

fun Route.cameraRoutes(service: CameraServicePort) {

    route("/cameras") {

        // ── LITERAL-PATH ROUTES FIRST (P5) ────────────────────────────────────────
        // Ktor prefers static segments over parameterised ones, but placing these
        // first makes the intent explicit and avoids "dashboard" being shadowed by
        // a future {id} reorder. Camera IDs that equal these words are unreachable
        // via GET /cameras/{id}; avoid creating cameras with those names.

        get("daily-stats/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                call.respond(service.getDailyStatsForAll(date))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        get("global-daily-rate/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                call.respond(service.getGlobalDailyCaptureRate(date))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        get("health-checks/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                call.respond(service.getAllCameraHealthChecks(date))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        get("dashboard/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                call.respond(service.getMonitoringDashboard(date))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        // ── COLLECTION ROUTES ──────────────────────────────────────────────────────

        get {
            try {
                call.respond(service.getAll())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        post {
            val dto = call.receive<CameraDto>()
            try {
                val id = service.create(dto)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        // ── SINGLE-CAMERA ROUTES ───────────────────────────────────────────────────

        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing camera id"))
            if (!id.isValidId())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid camera id"))

            try {
                val camera = service.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Camera '$id' not found"))
                call.respond(camera)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        put("{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing camera id"))
            if (!id.isValidId())
                return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid camera id"))

            val dto = call.receive<CameraDto>()
            try {
                val ok = service.update(id, dto)
                if (ok) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.NotFound, ErrorResponse("Camera '$id' not found"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing camera id"))
            if (!id.isValidId())
                return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid camera id"))

            try {
                val ok = service.delete(id)
                if (ok) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound, ErrorResponse("Camera '$id' not found"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        get("{id}/daily-stats/{date}") {
            val cameraId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing camera id"))
            if (!cameraId.isValidId())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid camera id"))
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                val stats = service.getDailyStats(cameraId, date)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Camera '$cameraId' not found"))
                call.respond(stats)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }

        get("{id}/health-check/{date}") {
            val cameraId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing camera id"))
            if (!cameraId.isValidId())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid camera id"))
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing date"))
            if (!date.isValidDate())
                return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date: $date. Expected yyyy-MM-dd"))

            try {
                val result = service.getCameraHealthCheck(cameraId, date)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Camera '$cameraId' not found"))
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Internal error"))
            }
        }
    }
}
