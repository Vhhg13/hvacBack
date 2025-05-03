package tk.vhhg.push

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.vhhg.table.Users

fun Route.pushRoutes() {
    fun ApplicationCall.getUserId() = principal<JWTPrincipal>()?.subject?.toInt()

    post("push") {
        val userId = call.getUserId()
        val token = call.receiveText()
        newSuspendedTransaction {
            Users.update({ Users.id eq userId }) {
                it[pushToken] = token
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}