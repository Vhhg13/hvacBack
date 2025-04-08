package tk.vhhg.rooms

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import tk.vhhg.table.Users

class RoomsRepositoryImpl : RoomsRepository {

    override suspend fun getRoomsForUser(userId: Int): List<RoomDto> = dbQuery {
        Room.selectAll().where { Room.ownerId eq userId }.map { room ->
            room.toRoomDto()
        }
    }

    override suspend fun postRoom(userId: Int, room: RoomDto): Long? = dbQuery {
        if (!userExists(userId)) return@dbQuery null
        val roomId = Room.insertAndGetId {
            it[ownerId] = userId
            it[name] = room.name
            it[color] = room.color
            it[volume] = room.volume
            it[scriptCode] = room.scriptCode
        }.value
        roomId
    }

    override suspend fun deleteRoom(userId: Int, roomId: Long): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        Room.deleteWhere { id eq roomId } == 1
    }

    override suspend fun patchRoom(userId: Int, patch: JsonObject): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        val roomId = patch["id"]?.jsonPrimitive?.long
        val rowsAffected = Room.update({ (Room.ownerId eq userId) and (Room.id eq roomId) }) { row ->
            patch["name"]?.let { row[name] = it.jsonPrimitive.content }
            patch["color"]?.let { row[color] = it.jsonPrimitive.content }
            patch["volume"]?.let { row[volume] = it.jsonPrimitive.float }
            patch["scriptCode"]?.let { row[scriptCode] = it.jsonPrimitive.content }
        }
        rowsAffected == 1
    }

    override suspend fun getRoomById(userId: Int, roomId: Long): RoomDto? = dbQuery {
        if (!userExists(userId)) return@dbQuery null
        val devices = Device.select(Device.id, Device.name, Device.type, Device.topic).where { Device.roomId eq roomId }.map {
            DeviceDto(
                id = it[Device.id].value,
                name = it[Device.name],
                type = it[Device.type],
                topic = it[Device.topic],
                roomId = roomId,
                historicData = null
            )
        }
        Room.selectAll().where { (Room.ownerId eq userId) and (Room.id eq roomId) }
            .singleOrNull()
            //?.toRoomDto(listOf(DeviceDto(id = 123, name = "Термокружка", type = "temp", roomId = roomId, topic = "", historicData = null)) + devices)
            ?.toRoomDto()
    }

    override suspend fun setTemperatureRegime(
        userId: Int,
        roomId: Long,
        target: Float?,
        deadline: Long?
    ): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        val count = Room.update({
            (Room.id eq roomId) and (Room.ownerId eq userId)
        }) {
            it[Room.targetTemp] = target
            it[Room.deadline] = deadline
        }
        count == 1
    }

    private fun userExists(userId: Int): Boolean {
        val user = Users.select(Users.id).where { Users.id eq userId }.singleOrNull()
        return user != null
    }

    private fun ResultRow.toRoomDto(devices: List<DeviceDto>? = null) = RoomDto(
        id = this[Room.id].value,
        name = this[Room.name],
        color = this[Room.color],
        volume = this[Room.volume],
        scriptCode = this[Room.scriptCode],
        deadline = this[Room.deadline],
        target = this[Room.targetTemp],
        devices = devices ?: emptyList()
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
}