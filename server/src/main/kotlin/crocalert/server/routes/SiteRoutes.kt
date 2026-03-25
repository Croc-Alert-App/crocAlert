package crocalert.server.routes

import crocalert.server.service.SiteServicePort
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.siteRoutes(service: SiteServicePort) {
    route("/sites") {
        get {
            call.respond(service.getAll())
        }
        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val site = service.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(site)
        }
    }
}
