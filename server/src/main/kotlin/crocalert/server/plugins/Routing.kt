package crocalert.server.plugins

import crocalert.server.routes.alertRoutes
import crocalert.server.service.AlertService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val alertService = AlertService()

    routing {
        alertRoutes(alertService)
    }
}