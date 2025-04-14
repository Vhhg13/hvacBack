package tk.vhhg.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Device : LongIdTable("device") {
    val type = varchar("type", 10)
    val ownerId = reference("owner_id", Users, onDelete = ReferenceOption.CASCADE)
    val topic = text("topic")
    val maxPower = float("max_power")
    val roomId = reference("room_id", Room, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
}