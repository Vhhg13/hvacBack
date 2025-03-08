package tk.vhhg.table

import org.jetbrains.exposed.dao.id.IntIdTable

object RefreshTokens : IntIdTable("refresh_tokens") {
    val value = varchar("value", 127)
    val isRevoked = bool("is_revoked").default(false)
}