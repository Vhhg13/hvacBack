package tk.vhhg

//import io.ktor.client.call.body
//import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
//import io.ktor.client.plugins.logging.DEFAULT
//import io.ktor.client.plugins.logging.LogLevel
//import io.ktor.client.plugins.logging.Logger
//import io.ktor.client.plugins.logging.Logging
//import io.ktor.client.request.delete
//import io.ktor.client.request.get
//import io.ktor.client.request.header
//import io.ktor.client.request.patch
//import io.ktor.client.request.post
//import io.ktor.client.request.setBody
//import io.ktor.client.statement.bodyAsText
//import io.ktor.http.ContentType
//import io.ktor.http.HttpHeaders
//import io.ktor.http.HttpStatusCode
//import io.ktor.http.contentType
//import io.ktor.serialization.kotlinx.json.json
//import io.ktor.server.testing.testApplication
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import tk.vhhg.rooms.model.RoomDto
//import tk.vhhg.table.Room
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFalse
//import kotlin.test.assertNotEquals
//import kotlin.test.assertTrue

//class MockRooms {
//    private var id = 1L
//    private fun randomRoom(): RoomDto {
//        id++
//        return RoomDto(
//            id = 0,
//            name = "room$id",
//            volume = 10F*id,
//            color = "#" + listOf("FF", "AA", "BB", "CC").shuffled().joinToString(""),
//            scriptCode = "$id"
//        )
//    }
//    @Test
//    fun mockRooms() = testApplication {
//        val client = createClient {
//            this@createClient.install(ContentNegotiation) {
//                json()
//            }
//            this@createClient.install(Logging) {
//                logger = Logger.DEFAULT
//                level = LogLevel.ALL
//            }
//        }
//
//        //val json = Json.parseToJsonElement().jsonObject
//        var accessToken = client.post("login") {
//            contentType(ContentType.Application.Json)
//            setBody("""{"username": "testUser", "password": "testPassword"}""")
//        }.let {
//            if (HttpStatusCode.OK == it.status) Json.decodeFromString<JsonObject>(it.bodyAsText()).get("access")?.jsonPrimitive?.content
//            else null
//        }.let {
//            it ?: ""
//        }
//        if (accessToken == "") {
//            accessToken = client.post("register") {
//                contentType(ContentType.Application.Json)
//                setBody("""{"username": "testUser", "password": "testPassword"}""")
//            }.let {
//                if (HttpStatusCode.OK == it.status) Json.decodeFromString<JsonObject>(it.bodyAsText())
//                    .get("access")?.jsonPrimitive?.content
//                else null
//            }.let {
//                it ?: throw Exception("Couldn't acquire token")
//            }
//        }
//
//        repeat(10) {
//            client.post("rooms") {
//                header(HttpHeaders.Authorization, "Bearer $accessToken")
//                contentType(ContentType.Application.Json)
//                setBody(randomRoom())
//            }.apply {
//                assertEquals(HttpStatusCode.OK, status)
//            }
//        }
//    }
//}