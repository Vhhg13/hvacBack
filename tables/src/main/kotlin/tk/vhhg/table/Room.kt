package tk.vhhg.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Room : LongIdTable("room") {
    val ownerId = reference("user", Users)
    val name = varchar("name", 255)
    val scriptCode = text("script_code")
    val volume = float("volume")
    val color = varchar("color", 9)
    val targetTemp = float("target_temp").nullable()
    val deadline = long("deadline").nullable()
}