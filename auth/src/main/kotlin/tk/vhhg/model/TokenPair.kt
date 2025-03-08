package tk.vhhg.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val access: String,
    val refresh: String
) {
    val bearer: String get() = "Bearer $access"
}