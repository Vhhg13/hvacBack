package tk.vhhg.plugins

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tk.vhhg.table.RefreshTokens
import tk.vhhg.table.Users

fun Application.configureDatabase() {
    val db = Database.connect(
        url = environment.config.property("postgres.url").getString(),
        user = environment.config.property("postgres.user").getString(),
        driver = "org.postgresql.Driver",
        password = environment.config.property("postgres.password").getString(),
    )
    transaction(db) {
        SchemaUtils.create(Users, RefreshTokens)
    }
}