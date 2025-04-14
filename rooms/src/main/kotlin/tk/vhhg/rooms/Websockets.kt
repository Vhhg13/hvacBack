package tk.vhhg.rooms

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import java.time.Duration
import java.time.temporal.ChronoUnit

fun Application.configureWebsockets() {
    install(WebSockets) {
        pingPeriod = Duration.of(15, ChronoUnit.SECONDS)
        timeout = Duration.of(15, ChronoUnit.SECONDS)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}