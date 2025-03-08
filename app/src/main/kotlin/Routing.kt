package tk.vhhg

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tk.vhhg.model.AuthRequest

fun Application.configureRouting() = routing {
    authRoutes()
    get("/") {
        call.respond("A" to "B")
    }
}