package tk.vhhg.rooms

import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(
    val id: Long,
    val color: String,
    val name: String,
    val volume: Float,
    val scriptCode: String
)