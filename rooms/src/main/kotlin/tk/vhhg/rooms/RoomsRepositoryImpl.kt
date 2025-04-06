package tk.vhhg.rooms

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
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

    override suspend fun patchRoom(userId: Int, room: RoomDto): Boolean = dbQuery {
        if (!userExists(userId)) return@dbQuery false
        val rowsAffected = Room.update({ (Room.ownerId eq userId) and (Room.id eq room.id) }) {
            it[name] = room.name
            it[color] = room.color
            it[volume] = room.volume
            it[scriptCode] = room.scriptCode
        }
        rowsAffected == 1
    }

    override suspend fun getRoomById(userId: Int, roomId: Long): RoomDto? = dbQuery {
        if (!userExists(userId)) return@dbQuery null
        Room.selectAll().where { (Room.ownerId eq userId) and (Room.id eq roomId) }
            .singleOrNull()
            ?.toRoomDto()
    }

    private fun userExists(userId: Int): Boolean {
        val user = Users.select(Users.id).where { Users.id eq userId }.singleOrNull()
        return user != null
    }

    private fun ResultRow.toRoomDto() = RoomDto(
        id = this[Room.id].value,
        name = this[Room.name],
        color = this[Room.color],
        volume = this[Room.volume],
        scriptCode = this[Room.scriptCode]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
}