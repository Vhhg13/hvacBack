package tk.vhhg

import io.ktor.server.application.*
import tk.vhhg.auth.configureSecurity
import tk.vhhg.plugins.configureDatabase
import tk.vhhg.plugins.configureMonitoring
import tk.vhhg.plugins.configureSerialization
import tk.vhhg.rooms.configureWebsockets

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDI()
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureDatabase()
    configureWebsockets()

    configureRouting()
}