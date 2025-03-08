package tk.vhhg.users

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.vhhg.model.TokenConfig
import tk.vhhg.model.TokenPair
import tk.vhhg.table.RefreshTokens
import tk.vhhg.table.Users
import java.time.Instant

class UserRepositoryImpl(val tokenConfig: TokenConfig, val tokenVerifier: JWTVerifier) : UserRepository {


    override suspend fun register(providedUsername: String, password: String): TokenPair? = dbQuery {
        val count = Users.select(Users.id).where { Users.username eq providedUsername }.count()
        if (count != 0L) return@dbQuery null

        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        val refreshToken = getRandomString(127)

        val refreshTokenId = RefreshTokens.insertAndGetId {
            it[value] = refreshToken
        }.value

        val id = Users.insertAndGetId {
            it[Users.username] = providedUsername
            it[Users.passwordHash] = hash
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
            it[RefreshTokens.isRevoked] = true
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
        val decodedAccessToken: DecodedJWT
        try {
            decodedAccessToken = tokenVerifier.verify(accessToken)
        } catch (e: JWTVerificationException) {
            return@dbQuery null
        }
        val username = decodedAccessToken.subject

        val refreshIdInToken = decodedAccessToken.getClaim("refresh").asInt()

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