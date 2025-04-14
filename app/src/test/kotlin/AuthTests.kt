package tk.vhhg

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import tk.vhhg.auth.model.AuthRequest
import tk.vhhg.auth.model.TokenPair
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthTests {

    @Test
    fun testRoot() = testApplication {
        val client = createClient {
            this@createClient.install(ContentNegotiation) {
                json()
            }
        }

        val username = randomString()
        val password = randomString()

        // 1. test successful register
        val tokenPairThatWillBeInvalidated: TokenPair
        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            tokenPairThatWillBeInvalidated = body()
        }

        // Другие тесты в том же файле...


        // 2. test register with existing username
        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.apply {
            assertEquals("Username taken", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }


        // 3. test login with wrong password
        client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password.dropLast(1)))
        }.apply {
            assertEquals("Incorrect credentials", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }


        // 4. test successful login
        var secondTokenPair: TokenPair
        client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            secondTokenPair = body()
        }


        // 5. test refresh with tokens invalidated by login (4.)
        client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(tokenPairThatWillBeInvalidated)
        }.apply {
            assertEquals("Invalid tokens", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }


        // 6. test refresh with valid tokens got from (4.)
        val invalidatedTokenPair: TokenPair = secondTokenPair
        client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(secondTokenPair)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            secondTokenPair = body()
        }


        // 7. test refresh with tokens already used once in (6.)
        client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(invalidatedTokenPair)
        }.apply {
            assertEquals("Invalid tokens", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }


        // 8. test protected handle with no token
        client.get("/auth").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }


        // 9. test protected handle with JWT
        client.get("/auth") {
            headers {
                append(HttpHeaders.Authorization, secondTokenPair.bearer)
            }
        }.apply {
            assertEquals(username, bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }


        // 9. test refresh with someone else's refresh token
        val secondUsername = randomString()
        val secondUser = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(secondUsername, randomString()))
        }
        assertEquals(HttpStatusCode.OK, secondUser.status)
        val anotherTokenPair: TokenPair = secondUser.body()

        val testRefreshWithSomeoneElsesTokens = client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(TokenPair(
                anotherTokenPair.access,
                secondTokenPair.refresh
            ))
        }
        assertEquals("Invalid tokens", testRefreshWithSomeoneElsesTokens.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, testRefreshWithSomeoneElsesTokens.status)
        client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(anotherTokenPair)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // 10. test refresh with token pair invalidated by login (again)
        client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.post("/refresh") {
            contentType(ContentType.Application.Json)
            setBody(secondTokenPair)
        }.apply {
            assertEquals("Invalid tokens", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }

    }

    private fun randomString(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val size = allowedChars.size
        return (1..20).map { allowedChars[Random.nextInt(size)] }.joinToString("")
    }

}
