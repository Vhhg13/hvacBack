package tk.vhhg.push

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val pushModule = module {
    single { PushNotificator(get(qualifier("broker"))) }
}