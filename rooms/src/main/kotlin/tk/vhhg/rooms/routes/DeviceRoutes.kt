package tk.vhhg.rooms.routes

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
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.StateFlow
import org.koin.ktor.ext.inject
import tk.vhhg.rooms.model.DeviceDto
import tk.vhhg.rooms.repo.DeviceRepository
import kotlin.coroutines.cancellation.CancellationException

fun Route.deviceRoutes() {

    val deviceRepo by inject<DeviceRepository>()

    fun ApplicationCall.getUserId() = principal<JWTPrincipal>()?.subject?.toInt()

    post("rooms/{room_id}/devices") {
        val userId = call.getUserId()!!
        val roomId = call.parameters["room_id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.BadRequest, "No room id provided")
            return@post
        }
        val device = call.receiveNullable<DeviceDto>()
        if (roomId != device?.roomId) {
            call.respond(HttpStatusCode.BadRequest, "room ids do not match")
            return@post
        }
        if (device.id != 0L) {
            call.respond(HttpStatusCode.BadRequest, "Device should have an id of 0")
            return@post
        }
        val createdDevice: DeviceDto? = deviceRepo.createDevice(userId, device)
        if (createdDevice == null) {
            call.respond(HttpStatusCode.NotFound, "Room not found by id")
        } else {
            call.respond(HttpStatusCode.OK, createdDevice)
        }
    }

    put("rooms/{room_id}/devices") {
        val userId = call.getUserId()!!
        val roomId = call.parameters["room_id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.BadRequest, "No room id provided")
            return@put
        }
        val device = call.receiveNullable<DeviceDto>()
        if (roomId != device?.roomId) {
            call.respond(HttpStatusCode.BadRequest, "room ids do not match")
            return@put
        }
        val success: Boolean = deviceRepo.putDevice(userId, device)
        if (success) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound, "Room or device not found")
        }
    }

    // Websocket

    delete("rooms/{room_id}/devices/{device_id}") {
        val userId = call.getUserId()!!
        val roomId = call.parameters["room_id"]?.toLong()
        val deviceId = call.parameters["device_id"]?.toLong()
        if (roomId == null || deviceId == null) {
            call.respond(HttpStatusCode.BadRequest, "roomId=$roomId, deviceId=$deviceId")
            return@delete
        }
        val success: Boolean = deviceRepo.deleteDevice(userId, deviceId)
        if (success) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound, "No device or room found")
        }
    }

    post("rooms/{room_id}/devices/{device_id}") {
        val userId = call.getUserId()!!
        val roomId = call.parameters["room_id"]?.toLong()
        val deviceId = call.parameters["device_id"]?.toLong()
        if (roomId == null || deviceId == null) {
            call.respond(HttpStatusCode.BadRequest, "roomId=$roomId, deviceId=$deviceId")
            return@post
        }
        val value = call.request.queryParameters["value"]?.toFloatOrNull()
        if (value == null) {
            call.respond(HttpStatusCode.BadRequest, "Illegal value provided")
            return@post
        }
        println(call.request.queryParameters)
        val success: Boolean = deviceRepo.setDeviceValue(userId, roomId, deviceId, value)
        if (success) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound, "Device or room not found")
        }
    }

    get("rooms/{room_id}/devices/{device_id}") {
        val userId = call.getUserId()!!
        val roomId = call.parameters["room_id"]?.toLong()
        val deviceId = call.parameters["device_id"]?.toLong()
        if (roomId == null || deviceId == null) {
            call.respond(HttpStatusCode.BadRequest, "roomId=$roomId, deviceId=$deviceId")
            return@get
        }

        val fromMillis = call.request.queryParameters["from"]?.toLong()
        val toMillis = call.request.queryParameters["to"]?.toLong()

        val device: DeviceDto?  = deviceRepo.getCurrentDeviceData(userId, roomId, deviceId, fromMillis, toMillis)
        if (device == null) {
            call.respond(HttpStatusCode.NotFound, "Room or device not found by id")
        } else {
            call.respond(HttpStatusCode.OK, device)
        }
    }

    webSocket("rooms/{room_id}/devices/{device_id}/live") {
        val userId = call.getUserId()!!
        println(call.parameters)
        val roomId = call.parameters["room_id"]?.toLong()
        if (roomId == null) {
            call.respond(HttpStatusCode.NotFound, "No room id provided")
            return@webSocket
        }
        val deviceId = call.parameters["device_id"]?.toLong()
        if (deviceId == null) {
            call.respond(HttpStatusCode.NotFound, "No deviceId provided")
            return@webSocket
        }
        val stateFlow: StateFlow<String>? = deviceRepo.getSubscription(userId, roomId, deviceId)
        if (stateFlow == null) {
            call.respond(HttpStatusCode.NotFound, "Room or device not found by id")
            return@webSocket
        }
//        val topic = deviceRepo.getTopicFor(deviceId)
        try {
            stateFlow.collect { value ->
                val frame = Frame.Text(value)
                outgoing.send(frame)
            }
        } catch (e: ClosedReceiveChannelException) {
            println("onClose ${closeReason.await()}")
        } catch (e: CancellationException) {
            println("WebSocket cancelled by client")
        } catch (e: Throwable) {
            println("onError ${closeReason.await()}")
            e.printStackTrace()
        }
    }

}