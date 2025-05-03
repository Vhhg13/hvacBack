package tk.vhhg

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.core.qualifier.qualifier
import org.koin.ktor.ext.get
import tk.vhhg.auth.authRoutes
import tk.vhhg.imitation.imitationRoutes
import tk.vhhg.push.pushRoutes
import tk.vhhg.rooms.routes.deviceRoutes
import tk.vhhg.rooms.routes.roomRoutes

fun Application.configureRouting() = routing {
    authRoutes()
    authenticate {
        imitationRoutes(get(qualifier("broker")))
        roomRoutes()
        deviceRoutes()
        pushRoutes()
    }
}