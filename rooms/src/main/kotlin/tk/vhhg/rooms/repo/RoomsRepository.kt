package tk.vhhg.rooms.repo

import kotlinx.serialization.json.JsonObject
import tk.vhhg.rooms.model.RoomDto

interface RoomsRepository {
    suspend fun getRoomsForUser(userId: Int): List<RoomDto>
    suspend fun postRoom(userId: Int, room: RoomDto): Long?
    suspend fun deleteRoom(userId: Int, roomId: Long): Boolean
    suspend fun patchRoom(userId: Int, room: JsonObject): Boolean
    suspend fun getRoomById(userId: Int, roomId: Long): RoomDto?
    suspend fun setTemperatureRegime(userId: Int, roomId: Long, target: Float?, deadline: Long?): Boolean?
}