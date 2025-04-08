package tk.vhhg.rooms

import kotlinx.serialization.json.JsonObject

interface RoomsRepository {
    suspend fun getRoomsForUser(userId: Int): List<RoomDto>
    suspend fun postRoom(userId: Int, room: RoomDto): Long?
    suspend fun deleteRoom(userId: Int, roomId: Long): Boolean
    suspend fun patchRoom(userId: Int, room: JsonObject): Boolean
    suspend fun getRoomById(userId: Int, roomId: Long): RoomDto?
    suspend fun setTemperatureRegime(userId: Int, roomId: Long, target: Float?, deadline: Long?): Boolean
}