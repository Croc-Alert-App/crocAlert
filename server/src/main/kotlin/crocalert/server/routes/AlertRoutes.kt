package crocalert.server.routes

import crocalert.server.dto.AlertDto
import crocalert.server.service.AlertService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.alertRoutes(alertService: AlertService) {
    get("/alerts") {
        call.respond(alertService.getAll())
    }

    post("/alerts") {
        val dto = call.receive<AlertDto>()
        val id = alertService.create(dto)
        call.respond(HttpStatusCode.Created, mapOf("id" to id))
    }
}