package tk.vhhg.rooms

import org.koin.dsl.module

val roomsModule = module {
    single<RoomsRepository> {
        RoomsRepositoryImpl()
    }
}