package tk.vhhg.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.koin.dsl.module
import tk.vhhg.auth.model.TokenConfig

val authModule = module {
    single {
        with(get<TokenConfig>()) {
            JWT
                .require(Algorithm.HMAC256(jwtSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtDomain)
                .build()
        }
    }
}