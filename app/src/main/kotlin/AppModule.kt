package tk.vhhg

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tk.vhhg.model.TokenConfig
import tk.vhhg.users.UserRepository
import tk.vhhg.users.UserRepositoryImpl

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

        single<UserRepository> { UserRepositoryImpl(get(), get()) }
    }
    install(Koin) {
        modules(authModule, appModule)
    }
}