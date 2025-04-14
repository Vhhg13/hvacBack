package tk.vhhg

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tk.vhhg.auth.authModule
import tk.vhhg.auth.model.TokenConfig
import tk.vhhg.auth.users.UserRepository
import tk.vhhg.auth.users.UserRepositoryImpl
import tk.vhhg.autocontrol.autocontrolModule
import tk.vhhg.rooms.roomsModule

fun Application.configureDI() {
    val appModule = module {
        single {
            TokenConfig(
                jwtAudience = environment.config.property("jwt.audience").getString(),
                jwtDomain = environment.config.property("jwt.domain").getString(),
                jwtSecret = environment.config.property("jwt.secret").getString(),
                jwtExpirationTimeSeconds = environment.config.property("jwt.expiration_time").getString().toLongOrNull()
                    ?: run {
                        throw IllegalArgumentException("JWT expiration time is non-numerical")
                    },
                jwtRealm = environment.config.property("jwt.realm").getString()
            )
        }

        single<UserRepository> { UserRepositoryImpl(get()) }

        single(qualifier("broker")) { environment.config.property("broker").getString() }
    }
    install(Koin) {
        modules(authModule, roomsModule, appModule, autocontrolModule)
    }
}