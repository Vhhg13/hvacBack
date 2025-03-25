package tk.vhhg.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject
import tk.vhhg.auth.model.AuthRequest
import tk.vhhg.auth.model.RefreshRequest
import tk.vhhg.auth.users.UserRepository


fun Routing.authRoutes() {
    val usersRepository by inject<UserRepository>()
    authenticate {
        get("/auth") {
            val user = call.principal<JWTPrincipal>()?.subject
            call.respond("$user")
        }
    }
    post("/register") {
        val req: AuthRequest? = call.receiveNullable()
        if (req == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val (username, password) = req

        val tokenPair = usersRepository.register(username, password)
        if (tokenPair == null) {
            call.respond(HttpStatusCode.BadRequest, "Username taken")
        } else {
            call.respond(HttpStatusCode.OK, tokenPair)
        }
    }
    post("/login") {
        val req: AuthRequest? = call.receiveNullable()
        if (req == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val (username, password) = req

        val tokenPair = usersRepository.login(username, password)
        if (tokenPair == null) {
            call.respond(HttpStatusCode.BadRequest, "Incorrect credentials")
        } else {
            call.respond(HttpStatusCode.OK, tokenPair)
        }
    }
    post("/refresh") {
        val req: RefreshRequest? = call.receiveNullable()
        if (req == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val (access, refresh) = req

        val tokenPair = usersRepository.refresh(access, refresh)
        if (tokenPair == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid tokens")
        } else {
            call.respond(HttpStatusCode.OK, tokenPair)
        }
    }
}