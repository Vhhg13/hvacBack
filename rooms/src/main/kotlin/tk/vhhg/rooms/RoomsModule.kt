package tk.vhhg.rooms

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import tk.vhhg.autocontrol.Broker
import tk.vhhg.rooms.repo.DeviceRepository
import tk.vhhg.rooms.repo.DeviceRepositoryImpl
import tk.vhhg.rooms.repo.RoomsRepository
import tk.vhhg.rooms.repo.RoomsRepositoryImpl
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

val roomsModule = module {

//    single {
//        Broker.instance(get(qualifier("broker")))
//    }

    single<RoomsRepository> {
        RoomsRepositoryImpl()
    }

    single<DeviceRepository> {
        DeviceRepositoryImpl(get())
    }
}