package tk.vhhg.rooms

import kotlinx.serialization.Serializable

@Serializable
data class TemperatureRegime(
    val target: Float? = null,
    val deadline: Long? = null
)
