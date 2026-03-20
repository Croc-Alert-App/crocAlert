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
    }
}