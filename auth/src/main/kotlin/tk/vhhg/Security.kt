package tk.vhhg

import com.auth0.jwt.JWTVerifier
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.ktor.ext.inject
import tk.vhhg.model.TokenConfig

fun Application.configureSecurity() {
    val tokenConfig: TokenConfig by inject()
    val jwtVerifier: JWTVerifier by inject()
    authentication {
        jwt {
            realm = tokenConfig.jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                if (credential.payload.audience.contains(tokenConfig.jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
