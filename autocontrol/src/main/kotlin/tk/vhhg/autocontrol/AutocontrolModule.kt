package tk.vhhg.autocontrol

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.qualifier.qualifier
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import tk.vhhg.autocontrol.Broker
import tk.vhhg.autocontrol.heatcool.HeaterCooler
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import java.util.concurrent.ConcurrentHashMap

val autocontrolModule = module {
//    val roomsLockQualifier = qualifier("roomsLock")
//    single<Map<Long, Boolean>>(roomsLockQualifier) { ConcurrentHashMap<Long, Boolean>() }
    single { HeaterCooler(get()) }
    single { Broker.instance(get(qualifier("broker"))) }
//    single<ScriptExecutor> {
//        val se = ScriptExecutor(get(), get<Map<Long, Boolean>>(roomsLockQualifier))
//        transaction {
//            println("instantiating se")
//            val q = Device.join(Room, JoinType.FULL, Device.roomId, Room.id)
//                .select(Room.id, Room.scriptCode, Device.topic, Device.id)
//                .orderBy(Device.id)
//                .groupBy(
//                    keySelector = {
//                        it[Room.id].value to it[Room.scriptCode]
//                    },
//                    valueTransform = { it.getOrNull(Device.topic) }
//                )
//            println("scripts in db: $q")
//            se.scheduleAll(q)
//        }
//        se
//    }
}