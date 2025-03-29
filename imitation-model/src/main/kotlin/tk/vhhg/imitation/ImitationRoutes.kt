package tk.vhhg.imitation

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

fun Route.imitationRoutes(brokerUrl: String) {
    val imitator = Imitator(brokerUrl)

    transaction {
        SchemaUtils.create(ImitatedRoom)
        ImitatedRoom.selectAll().map { it ->
            ImitatedRoomDto(
                id = it[ImitatedRoom.id].value,
                heaters = it[ImitatedRoom.heaters],
                coolers = it[ImitatedRoom.coolers],
                volume = it[ImitatedRoom.volume],
                out = it[ImitatedRoom.out],
                k = it[ImitatedRoom.k],
                thermostat =it[ImitatedRoom.thermostat]
            )
        }.forEach {
            imitator.imitate(it)
        }
    }

    delete("/im/{id}") {
        val id = call.parameters["id"]!!.toInt()
        imitator.drop(id)
        dbQuery {
            ImitatedRoom.deleteWhere { ImitatedRoom.id eq id }
        }
        call.respond(HttpStatusCode.OK)
    }

    get("/im") {
        dbQuery {
            val list = ImitatedRoom.selectAll().map { ImitatedRoomDto(
                id = it[ImitatedRoom.id].value,
                heaters = it[ImitatedRoom.heaters],
                coolers = it[ImitatedRoom.coolers],
                volume = it[ImitatedRoom.volume],
                out = it[ImitatedRoom.out],
                k = it[ImitatedRoom.k],
                thermostat = it[ImitatedRoom.thermostat]
            ) }
            call.respond(list)
        }
    }

    put("/im") {
        val body = call.receive<ImitatedRoomDto>()
        dbQuery {
            ImitatedRoom.upsert {
                it[id] = body.id
                it[heaters] = body.heaters
                it[coolers] = body.coolers
                it[volume] = body.volume
                it[out] = body.out
                it[k] = body.k
                it[thermostat] = body.thermostat
            }
        }
        imitator.imitate(body)
        call.respond(HttpStatusCode.OK)
    }
}
private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }