package crocalert.server.routes

import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.service.CameraServicePort
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cameraRoutes(service: CameraServicePort) {

    route("/cameras") {

        get {
            call.respond(service.getAll())
        }

        post {
            val dto = call.receive<CameraDto>()
            val id = service.create(dto)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        get("{id}") {

            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val camera = service.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(camera)
        }

        put("{id}") {

            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest)

            val dto = call.receive<CameraDto>()

            val ok = service.update(id, dto)

            if (ok)
                call.respond(HttpStatusCode.OK)
            else
                call.respond(HttpStatusCode.NotFound)
        }

        delete("{id}") {

            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest)

            val ok = service.delete(id)

            if (ok)
                call.respond(HttpStatusCode.NoContent)
            else
                call.respond(HttpStatusCode.NotFound)
        }
        get("{id}/daily-stats/{date}") {
            val cameraId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing camera id")

            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            val stats = service.getDailyStats(cameraId, date)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Camera not found")

            call.respond(stats)
        }

        get("daily-stats/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            call.respond(service.getDailyStatsForAll(date))
        }
        get("global-daily-rate/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            val stats = service.getGlobalDailyCaptureRate(date)
            call.respond(stats)
        }
        //una cam
        get("{id}/health-check/{date}") {
            val cameraId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing camera id")

            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            val result = service.getCameraHealthCheck(cameraId, date)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Camera not found")

            call.respond(result)
        }

        //todas las cams
        get("health-check/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            call.respond(service.getAllCameraHealthChecks(date))
        }

        // 🔥 DASHBOARD COMPLETO (el importante)
        get("dashboard/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing date")

            call.respond(service.getMonitoringDashboard(date))
        }


    }
}