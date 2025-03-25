package tk.vhhg.auth.table

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("Users") {
    val username = varchar("username", 127)
    val passwordHash = varchar("pwd_hash", 60)
    val refreshToken = reference("refresh_token", id)
    val pushToken = text("push_token").nullable()
}