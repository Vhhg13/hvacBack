package tk.vhhg.imitation

import kotlinx.serialization.Serializable

@Serializable
data class ImitatedRoomDto(
    val id: Int,
    val heaters: String,
    val coolers: String,
    val volume: Float,
    val out: Float,
    val k: Float,
    val thermostat: String,
)
