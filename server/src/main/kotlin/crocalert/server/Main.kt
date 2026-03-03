package crocalert.server

import crocalert.server.plugins.configureSerialization
import crocalert.server.routes.alertRoutes
import crocalert.server.service.AlertService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
    FirebaseInit.init()

    val alertService = AlertService()

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        configureSerialization()

        routing {
            alertRoutes(alertService)
        }
    }.start(wait = true)
}