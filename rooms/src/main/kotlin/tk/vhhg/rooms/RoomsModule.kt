package tk.vhhg.rooms

import org.koin.dsl.module
import tk.vhhg.rooms.repo.DeviceRepository
import tk.vhhg.rooms.repo.DeviceRepositoryImpl
import tk.vhhg.rooms.repo.RoomsRepository
import tk.vhhg.rooms.repo.RoomsRepositoryImpl

val roomsModule = module {
    single<RoomsRepository> {
        RoomsRepositoryImpl(get(), get())
    }

    single<DeviceRepository> {
        DeviceRepositoryImpl(get(), get(), get())
    }
}