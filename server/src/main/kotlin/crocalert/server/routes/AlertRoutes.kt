package crocalert.server.routes

import crocalert.server.service.AlertService
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.request.*
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


    }
}