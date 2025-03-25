package tk.vhhg.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    val access: String,
    val refresh: String
)
