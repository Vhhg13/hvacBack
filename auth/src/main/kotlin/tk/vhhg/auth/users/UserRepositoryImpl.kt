package tk.vhhg.auth.users

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.vhhg.auth.model.TokenConfig
import tk.vhhg.auth.model.TokenPair
import tk.vhhg.auth.table.RefreshTokens
import tk.vhhg.auth.table.Users
import java.time.Instant
import java.util.*

class UserRepositoryImpl(val tokenConfig: TokenConfig) : UserRepository {


    override suspend fun register(providedUsername: String, password: String): TokenPair? = dbQuery {
        val count = Users.select(Users.id).where { Users.username eq providedUsername }.count()
        if (count != 0L) return@dbQuery null

        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        val refreshToken = getRandomString(127)

        val refreshTokenId = RefreshTokens.insertAndGetId {
            it[value] = refreshToken
        }.value

        Users.insert {
            it[username] = providedUsername
            it[passwordHash] = hash
            it[Users.refreshToken] = refreshTokenId
        }

        val pair = createTokenPair(providedUsername, refreshTokenId, refreshToken)
        return@dbQuery pair
    }

    override suspend fun login(providedUsername: String, password: String): TokenPair? = dbQuery {
        val user =
            Users.select(Users.id, Users.passwordHash, Users.refreshToken).where { Users.username eq providedUsername }
                .singleOrNull()
        if (user == null) return@dbQuery null

        val passwordHash = user[Users.passwordHash]
        val verified = BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified
        if (!verified) return@dbQuery null

        // password is now verified

        val oldRefreshTokenId = user[Users.refreshToken].value
        RefreshTokens.update({ RefreshTokens.id eq oldRefreshTokenId }) {
            it[isRevoked] = true
        }

        val refreshToken = getRandomString(127)
        val refreshTokenId = RefreshTokens.insertAndGetId {
            it[value] = refreshToken
        }.value

        Users.update({ Users.id eq user[Users.id] }) {
            it[Users.refreshToken] = refreshTokenId
        }

        createTokenPair(providedUsername, refreshTokenId, refreshToken)
    }

    override suspend fun refresh(accessToken: String, refreshToken: String): TokenPair? = dbQuery {
        val payload = String(Base64.getDecoder().decode(accessToken.substringAfter('.').substringBefore('.')))
        val decodedJwt = Json.decodeFromString<JsonObject>(payload)
        val username = decodedJwt["sub"]!!.jsonPrimitive.content
        val refreshIdInToken = decodedJwt["refresh"]?.jsonPrimitive?.int

        val refreshRow = RefreshTokens
            .select(RefreshTokens.id, RefreshTokens.isRevoked)
            .where { RefreshTokens.value eq refreshToken }
            .singleOrNull()

        val isRevoked = refreshRow?.get(RefreshTokens.isRevoked)
        if (isRevoked != false) return@dbQuery null

        val refreshIdInDb = refreshRow[RefreshTokens.id].value

        if (refreshIdInDb == refreshIdInToken != true)
            return@dbQuery null

        val refreshToken = getRandomString(127)
        val refreshTokenId = RefreshTokens.insertAndGetId {
            it[value] = refreshToken
        }.value
        RefreshTokens.update({ RefreshTokens.id eq refreshIdInDb }) {
            it[RefreshTokens.isRevoked] = true
        }
        Users.update({ Users.username eq username }) {
            it[Users.refreshToken] = refreshTokenId
        }
        createTokenPair(username, refreshTokenId, refreshToken)
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun createTokenPair(username: String, refreshId: Int, refresh: String): TokenPair {
        val access = JWT.create()
            .withAudience(tokenConfig.jwtAudience)
            .withIssuer(tokenConfig.jwtDomain)
            .withExpiresAt(Instant.now().plusSeconds(tokenConfig.jwtExpirationTimeSeconds))
            .withSubject(username)
            .withClaim("refresh", refreshId)
            .sign(Algorithm.HMAC256(tokenConfig.jwtSecret))
        return TokenPair(access, refresh)
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }
}