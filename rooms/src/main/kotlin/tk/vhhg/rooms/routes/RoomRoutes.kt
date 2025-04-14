package tk.vhhg.rooms.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject
import org.koin.ktor.ext.inject
import tk.vhhg.rooms.model.RoomDto
import tk.vhhg.rooms.repo.RoomsRepository
import tk.vhhg.rooms.model.TemperatureRegime

fun Route.roomRoutes() {
    val roomRepo by inject<RoomsRepository>()

    fun ApplicationCall.getUserId() = principal<JWTPrincipal>()?.subject?.toInt()

    post("rooms/{id}/temperature") {
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User not found")
            return@post
        }
        val body = call.receiveNullable<TemperatureRegime>()
        if (body == null) {
            call.respond(HttpStatusCode.BadRequest, "Malformed request")
            return@post
        }
        val roomId = call.parameters["id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.NotFound, "No id provided")
            return@post
        }
        val success = roomRepo.setTemperatureRegime(userId, roomId, body.target, body.deadline)
        when (success) {
            true -> call.respond(HttpStatusCode.OK)
            false -> call.respond(HttpStatusCode.NotFound, "Room not found by id")
            null -> call.respond(HttpStatusCode.Conflict, "No thermostat in room")
        }
//        if (success) {
//            call.respond(HttpStatusCode.OK)
//        } else {
//            call.respond(HttpStatusCode.NotFound, "Room not found by id")
//        }
    }
    delete("rooms/{id}") {
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User not found")
            return@delete
        }
        val roomId = call.parameters["id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.NotFound, "Room not found")
            return@delete
        }
        roomRepo.deleteRoom(userId = userId, roomId = roomId)
        call.respond(HttpStatusCode.OK)
    }
    get("rooms/{id}") {
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User not found")
            return@get
        }
        val roomId = call.parameters["id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.BadRequest, "No room id provided")
            return@get
        }
        val room = roomRepo.getRoomById(userId, roomId)
        if (room == null) {
            call.respond(HttpStatusCode.NotFound, "Room not found")
        } else {
            call.respond(HttpStatusCode.OK, room)
        }
    }
    patch("rooms") {
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "No userId provided")
            return@patch
        }
        val patch = call.receiveNullable<JsonObject>()
        if (patch == null) {
            call.respond(HttpStatusCode.BadRequest, "Malformed room")
            return@patch
        }
        val success = roomRepo.patchRoom(userId, patch)
        if (success) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound, "No room found")
        }
    }
    get("rooms") {
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "No userId provided")
            return@get
        }
        val list = roomRepo.getRoomsForUser(userId)
        call.respond(HttpStatusCode.OK, list)
    }

    post("rooms") {
        val room = call.receiveNullable<RoomDto>()
        if (room == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid room")
            return@post
        } else if (room.id != 0L) {
            call.respond(HttpStatusCode.BadRequest, "Room id must be 0 when adding new room")
            return@post
        }
        val userId = call.getUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.BadRequest, "No userId provided")
            return@post
        }
        val roomId = roomRepo.postRoom(userId, room)
        if (roomId == null) {
            call.respond(HttpStatusCode.NotFound, "Room not found")
        } else {
            call.respond(HttpStatusCode.OK, room.copy(id = roomId))
        }
    }
}