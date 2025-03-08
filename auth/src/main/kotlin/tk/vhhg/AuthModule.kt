package tk.vhhg

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.koin.dsl.module
import tk.vhhg.model.TokenConfig

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