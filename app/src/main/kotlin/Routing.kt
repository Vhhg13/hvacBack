package tk.vhhg

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.qualifier.qualifier
import org.koin.ktor.ext.get
import tk.vhhg.auth.authRoutes
import tk.vhhg.imitation.imitationRoutes

fun Application.configureRouting() = routing {
    authRoutes()
    imitationRoutes(get(qualifier("broker")))
    get("/") {
        call.respond("A" to "B")
    }
}