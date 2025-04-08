package tk.vhhg.rooms

import kotlinx.serialization.Serializable

@Serializable
data class PieceOfHistory(
    val time: Long,
    val value: String
)