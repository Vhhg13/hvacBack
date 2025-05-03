package tk.vhhg.rooms.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.vhhg.autocontrol.heatcool.HeatCoolDevice
import tk.vhhg.autocontrol.heatcool.HeaterCooler
import tk.vhhg.autocontrol.scripting.ScriptExecutor
import tk.vhhg.rooms.model.DeviceDto
import tk.vhhg.rooms.model.RoomDto
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import tk.vhhg.table.Users

class RoomsRepositoryImpl(
    private val scriptExecutor: ScriptExecutor,
    private val heaterCooler: HeaterCooler
) : RoomsRepository {

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
            it[scriptCode] = ""
        }.value
        scriptExecutor.runAll()
        roomId
    }

    override suspend fun deleteRoom(userId: Int, roomId: Long): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        //scriptExecutor.runAll()
        scriptExecutor.remove(roomId)
        setTemperatureRegime(userId, roomId, null, null)
        Room.deleteWhere { id eq roomId } == 1
    }

    override suspend fun patchRoom(userId: Int, patch: JsonObject): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        val roomId = patch["id"]?.jsonPrimitive?.long
        println(patch)
        val rowsAffected = Room.update({ (Room.ownerId eq userId) and (Room.id eq roomId) }) { row ->
            patch["name"]?.let { row[name] = it.jsonPrimitive.content }
            patch["color"]?.let { row[color] = it.jsonPrimitive.content }
            patch["volume"]?.let { row[volume] = it.jsonPrimitive.float }
            patch["scriptCode"]?.let { row[scriptCode] = it.jsonPrimitive.content }
        }
        patch["scriptCode"]?.let {
            if (roomId != null) scriptExecutor.runAll()
        }
        rowsAffected == 1
    }

    override suspend fun getRoomById(userId: Int, roomId: Long): RoomDto? = dbQuery {
        if (!userExists(userId)) return@dbQuery null
        val devices = Device.select(Device.id, Device.name, Device.type, Device.topic, Device.maxPower)
            .where { (Device.roomId eq roomId) and (Device.ownerId eq userId) }.orderBy(Device.id).map {
                DeviceDto(
                    id = it[Device.id].value,
                    name = it[Device.name],
                    type = it[Device.type],
                    topic = it[Device.topic],
                    roomId = roomId,
                    historicData = null,
                    maxPower = it[Device.maxPower]
                )
            }
        Room.selectAll().where { (Room.ownerId eq userId) and (Room.id eq roomId) }
            .singleOrNull()
            ?.toRoomDto(devices)
    }

    override suspend fun setTemperatureRegime(
        userId: Int,
        roomId: Long,
        target: Float?,
        deadline: Long?
    ): Boolean? = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        val volume = Room.select(Room.volume).where { (Room.id eq roomId) and (Room.ownerId eq userId) }.singleOrNull()?.get(
            Room.volume)
         ?: return@dbQuery false
        val count = Room.update({
            (Room.id eq roomId) and (Room.ownerId eq userId)
        }) {
            it[targetTemp] = target
            it[Room.deadline] = deadline
        }
        val devices = Device.join(Room, JoinType.FULL, Device.roomId, Room.id)
            .select(Device.topic, Device.maxPower, Device.type, Device.ownerId)
            .where { (Room.id eq roomId) and (Device.id.isNotNull()) }
            .orderBy(Device.type to SortOrder.DESC_NULLS_LAST, Device.id to SortOrder.ASC_NULLS_LAST)
            .map {
                HeatCoolDevice(
                    topic = it[Device.topic],
                    type = it[Device.type],
                    maxPower = it[Device.maxPower].toDouble()
                )
            }
        heaterCooler.start(
            roomId = roomId,
            volume = volume,
            target = target,
            deadline = deadline,
            devices = devices
        )

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