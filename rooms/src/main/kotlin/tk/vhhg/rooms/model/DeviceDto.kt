package tk.vhhg.rooms.model

import kotlinx.serialization.Serializable
import tk.vhhg.rooms.model.PieceOfHistory

@Serializable
data class DeviceDto(
    val id: Long,
    val name: String,
    val type: String,
    val roomId: Long,
    val historicData: List<PieceOfHistory>?,
    val topic: String,
    val maxPower: Float
)