package tk.vhhg.autocontrol

import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import tk.vhhg.autocontrol.heatcool.HeaterCooler
import tk.vhhg.autocontrol.scripting.ScriptExecutor
import tk.vhhg.table.Device
import java.util.concurrent.ConcurrentHashMap

val autocontrolModule = module {
    val roomsLockQualifier = qualifier("roomsLock")
    single(roomsLockQualifier) { ConcurrentHashMap<Long, Boolean>() }
    single { HeaterCooler(get(), get(roomsLockQualifier)) }
    single<Broker> {
        transaction {
            val broker = Broker.instance(get(qualifier("broker")))
            for (row in Device.select(Device.topic)) {
                broker.subscribe(row[Device.topic])
            }
            broker
        }
    }
    single<ScriptExecutor> {
        ScriptExecutor(get(), get(roomsLockQualifier)).apply { runAll() }
    }
}