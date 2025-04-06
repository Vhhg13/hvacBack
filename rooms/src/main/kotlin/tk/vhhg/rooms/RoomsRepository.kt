package tk.vhhg.rooms

interface RoomsRepository {
    suspend fun getRoomsForUser(userId: Int): List<RoomDto>
    suspend fun postRoom(userId: Int, room: RoomDto): Long?
    suspend fun deleteRoom(userId: Int, roomId: Long): Boolean
    suspend fun patchRoom(userId: Int, room: RoomDto): Boolean
    suspend fun getRoomById(userId: Int, roomId: Long): RoomDto?
}