package crocalert.server.plugins

import crocalert.server.routes.AlertDto
import crocalert.server.routes.alertRoutes
import crocalert.server.service.AlertService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.alertRoutes(service: AlertService) {
    route("/alerts") {
        get {
            call.respond(service.getAll())
        }

        post {
            val dto = call.receive<AlertDto>()
            val id = service.create(dto)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val alert = service.getById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(alert)
        }

        put("{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val dto = call.receive<AlertDto>()
            val ok = service.update(id, dto)
            if (ok) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.NotFound)
        }

        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val ok = service.delete(id)
            if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}