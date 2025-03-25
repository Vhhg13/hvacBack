package tk.vhhg.imitation

import org.jetbrains.exposed.dao.id.IntIdTable

internal object ImitatedRoom: IntIdTable("imitated_room") {
    val heaters = text("heaters")
    val coolers = text("coolers")
    val volume = float("volume")
    val k = float("k")
    val out = float("out")
    val thermostat = text("thermostat")
}