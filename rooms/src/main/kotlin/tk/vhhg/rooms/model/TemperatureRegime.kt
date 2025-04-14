package tk.vhhg.rooms.model

import kotlinx.serialization.Serializable

@Serializable
data class TemperatureRegime(
    val target: Float? = null,
    val deadline: Long? = null
)