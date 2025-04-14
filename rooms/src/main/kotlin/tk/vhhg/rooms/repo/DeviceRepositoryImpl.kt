package tk.vhhg.rooms.repo

import tk.vhhg.autocontrol.Broker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteReturning
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.vhhg.autocontrol.heatcool.HeatCoolDevice
import tk.vhhg.autocontrol.heatcool.HeaterCooler
import tk.vhhg.rooms.model.DeviceDto
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import tk.vhhg.table.Users

class DeviceRepositoryImpl(
    private val broker: Broker,
    private val heaterCooler: HeaterCooler
) : DeviceRepository {
    override suspend fun createDevice(userId: Int, deviceDto: DeviceDto): DeviceDto? = dbQuery {
        if (!userExists(userId)) return@dbQuery null
        val id = Device.insertAndGetId {
            it[type] = deviceDto.type
            it[ownerId] = userId
            it[topic] = deviceDto.topic
            it[maxPower] = deviceDto.maxPower
            it[roomId] = deviceDto.roomId
            it[name] = deviceDto.name
        }.value
//        val q = Device.join(Room, JoinType.FULL, Device.roomId, Room.id)
//            .select(Room.id, Room.scriptCode, Device.topic, Device.id)
//            .orderBy(Device.id)
//            .groupBy(
//                keySelector = {
//                    it[Room.id].value to it[Room.scriptCode]
//                },
//                valueTransform = { it.getOrNull(Device.topic) }
//            )
        //scriptExecutor.scheduleAll(q)
        deviceDto.copy(id = id)
    }

    override suspend fun putDevice(userId: Int, deviceDto: DeviceDto): Boolean = dbQuery {
        val id = Device.select(Device.id).where { Device.id eq deviceDto.id }.singleOrNull()?.get(Device.id)?.value
        if (id == null) return@dbQuery false
        Device.update({ Device.id eq id }) {
            it[name] = deviceDto.name
            it[type] = deviceDto.type
            it[roomId] = deviceDto.roomId
            it[topic] = deviceDto.topic
            it[maxPower] = deviceDto.maxPower
        }
//        val q = Device.join(Room, JoinType.FULL, Device.roomId, Room.id)
//            .select(Room.id, Room.scriptCode, Device.topic, Device.id)
//            .orderBy(Device.id)
//            .groupBy(
//                keySelector = {
//                    it[Room.id].value to it[Room.scriptCode]
//                },
//                valueTransform = { it.getOrNull(Device.topic) }
//            )
//        scriptExecutor.scheduleAll(q)
        true
    }

    override suspend fun deleteDevice(userId: Int, deviceId: Long): Boolean = dbQuery {
        val removedRow = Device.deleteReturning(listOf(Device.roomId, Device.topic)) { Device.id eq deviceId }.singleOrNull()
        if (removedRow == null) return@dbQuery false
        broker[removedRow[Device.topic]] = "0"
        broker.unsubscribe(removedRow[Device.topic])
        //scriptExecutor.removeTopic(removedRow[Device.roomId].value, removedRow[Device.topic])
        true
    }

    override suspend fun setDeviceValue(
        userId: Int,
        roomId: Long,
        deviceId: Long,
        value: Float
    ): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
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
        heaterCooler.start(roomId, TODO(), null, null, devices)
        val topic = Device.select(Device.topic)
            .where { (Device.id eq deviceId) and (Device.roomId eq roomId) and (Device.ownerId eq userId) }
            .singleOrNull()?.get(Device.topic)
        topic?.let {
            broker[topic] = value.toString()
        }
        topic != null
    }

    override suspend fun getCurrentDeviceData(
        userId: Int,
        roomId: Long,
        deviceId: Long,
        fromMillis: Long?,
        toMillis: Long?
    ): DeviceDto? = dbQuery {
        val deviceRow = Device.selectAll().where { (Device.id eq deviceId) and (Device.ownerId eq userId) and (Device.roomId eq roomId) }.singleOrNull()

        deviceRow?.let {
            DeviceDto(
                id = it[Device.id].value,
                name = it[Device.name],
                type = it[Device.type],
                roomId = it[Device.roomId].value,
                historicData = emptyList(),
                topic = it[Device.topic],
                maxPower = it[Device.maxPower],
            )
        }
    }

    override suspend fun getTopicFor(deviceId: Long): String? = dbQuery {
        Device.select(Device.topic).where { Device.id eq deviceId }.singleOrNull()?.get(Device.topic)
    }

    //    override suspend fun getSubscription(
//        userId: Int,
//        roomId: Long,
//        deviceId: Long
//    ): StateFlow<String>? = dbQuery {
//        Device.select(Device.topic)
//            .where { (Device.id eq deviceId) and (Device.roomId eq roomId) and (Device.ownerId eq userId)}
//            .singleOrNull()?.get(Device.topic)?.let { topic ->
//                broker.subscribe(topic)
//            }
//    }

    private fun userExists(userId: Int): Boolean {
        val user = Users.select(Users.id).where { Users.id eq userId }.singleOrNull()
        return user != null
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
}