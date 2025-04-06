package tk.vhhg.rooms

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.roomRoutes() {
    val roomRepo by inject<RoomsRepository>()

    fun ApplicationCall.getUserId() = principal<JWTPrincipal>()?.subject?.toInt()

    post("rooms/{id}/temperature") {
        TODO("Later")
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
        val room = call.receiveNullable<RoomDto>()
        if (room == null) {
            call.respond(HttpStatusCode.BadRequest, "Malformed room")
            return@patch
        }
        val success = roomRepo.patchRoom(userId, room)
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