package tk.vhhg.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Device : LongIdTable("device") {
    val type = varchar("type", 10)
    val ownerId = reference("owner_id", Users)
    val topic = text("topic")
    val maxPower = float("max_power")
    val roomId = reference("room_id", Room)
    val name = text("name")
}