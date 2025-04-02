package tk.vhhg.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Room : LongIdTable("room") {
    val ownerId = reference("user", Users)
    val name = varchar("name", 255)
    val scriptCode = text("script_code")
    val volume = float("volume")
}