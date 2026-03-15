package crocalert.server.routes

import crocalert.app.shared.data.dto.CaptureDto
import crocalert.server.service.CaptureService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.captureRoutes(service: CaptureService) {
    route("/captures") {

        get {
            call.respond(service.getAll())
        }

        post {
            val dto = call.receive<CaptureDto>()
            val id = service.create(dto)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")

            val capture = service.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Capture not found")

            call.respond(capture)
        }

        put("{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")

            val dto = call.receive<CaptureDto>()
            val updated = service.update(id, dto)

            if (updated) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Capture updated"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Capture not found")
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

            val deleted = service.delete(id)

            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Capture not found")
            }
        }
    }
}