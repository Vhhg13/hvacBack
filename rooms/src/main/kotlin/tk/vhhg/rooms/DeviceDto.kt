package tk.vhhg.rooms

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: Long,
    val name: String,
    val type: String,
    val roomId: Long,
    val historicData: List<PieceOfHistory>?,
    val topic: String
)
