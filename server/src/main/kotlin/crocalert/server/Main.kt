package crocalert.server

import crocalert.server.plugins.configureSerialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {

    FirebaseInit.init()   // 👈 IMPORTANTE

    embeddedServer(Netty, port = 8080) {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}