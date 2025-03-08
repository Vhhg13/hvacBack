package tk.vhhg.model

data class TokenConfig(
    val jwtAudience: String,
    val jwtDomain: String,
    val jwtSecret: String,
    val jwtExpirationTimeSeconds: Long,
    val jwtRealm: String
)