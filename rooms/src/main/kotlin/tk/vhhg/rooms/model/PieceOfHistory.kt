package tk.vhhg.rooms.model

import kotlinx.serialization.Serializable

@Serializable
data class PieceOfHistory(
    val time: Long,
    val value: String
)