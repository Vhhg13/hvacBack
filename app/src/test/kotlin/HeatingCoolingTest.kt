//package tk.vhhg
//
//import io.ktor.client.HttpClient
//import io.ktor.client.call.body
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.client.plugins.logging.*
//import io.ktor.client.plugins.websocket.WebSockets
//import io.ktor.client.plugins.websocket.webSocket
//import io.ktor.client.request.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import io.ktor.server.testing.*
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.collect
//import kotlinx.coroutines.launch
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonObject
//import kotlinx.serialization.json.buildJsonObject
//import kotlinx.serialization.json.float
//import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.serialization.json.put
//import tk.vhhg.autocontrol.Broker
//import tk.vhhg.rooms.model.DeviceDto
//import tk.vhhg.rooms.model.RoomDto
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class HeatingCoolingTest {
//    @Test
//    fun testRoot() = testApplication {
//        val (client, accessToken) = getClientAndAccessToken()
//        val broker = Broker.instance("tcp://0.0.0.0:1883")
//
//        client.put("im") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody("""{
// "id": 100,
// "heaters": "h1",
// "coolers": "c1",
// "volume": 5,
// "out": 18,
// "k": 0.2,
// "thermostat": "t1"
//}""")
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//        }
//
//        val room = postRandomRoom(client, accessToken, 100F)
//
//        val tempId = client.post("rooms/${room.id}/devices") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody(DeviceDto(
//                id = 0,
//                name = "device0",
//                type = "temp",
//                roomId = room.id,
//                historicData = null,
//                topic = "t1",
//                maxPower = 0F,
//            ))
//        }.let {
//            assertEquals(HttpStatusCode.OK, it.status)
//            it.body<DeviceDto>().id
//        }
//
//        val maxHeat = 5000F
//        val heatId = client.post("rooms/${room.id}/devices") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody(DeviceDto(
//                id = 0,
//                name = "device1",
//                type = "heat",
//                roomId = room.id,
//                historicData = null,
//                topic = "h1",
//                maxPower = maxHeat,
//            ))
//        }.let {
//            assertEquals(HttpStatusCode.OK, it.status)
//            it.body<DeviceDto>().id
//        }
//
//        val maxCool = 4000F
//        val coolId = client.post("rooms/${room.id}/devices") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody(DeviceDto(
//                id = 0,
//                name = "device0",
//                type = "cool",
//                roomId = room.id,
//                historicData = null,
//                topic = "c1",
//                maxPower = maxCool,
//            ))
//        }.let {
//            assertEquals(HttpStatusCode.OK, it.status)
//            it.body<DeviceDto>().id
//        }
//
//        println(accessToken)
//
//        return@testApplication
//
//        val exampleTarget = 20F
//        client.post("rooms/${room.id}/temperature") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody(buildJsonObject {
//                put("target", exampleTarget)
//            })
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//        }
//        println("here 1")
//        delay(5000)
//        assertEquals(maxHeat, broker["h1"].toFloat())
//        delay(5500)
//        println("here 2")
//        assertTrue { broker["t1"].toFloat() > 18F }
//        println("here 3")
//
//        client.get("rooms/${room.id}"){
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//            val body = body<RoomDto>()
//            assertEquals(exampleTarget, body.target)
//        }
//
//        client.post("rooms/${room.id}/temperature") {
//            appJson()
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            setBody(buildJsonObject {})
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//        }
//
//        client.get("rooms/${room.id}"){
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//            val body = body<RoomDto>()
//            assertEquals(null, body.target)
//        }
//
//        delay(5000)
//        assertEquals("0", broker["h1"])
//        println("here 4")
//    }
//
//    private suspend fun ApplicationTestBuilder.getClientAndAccessToken(): Pair<HttpClient, String> {
//        val client = createClient {
//            this@createClient.install(ContentNegotiation) { json() }
//            this@createClient.install(Logging) {
//                logger = Logger.DEFAULT
//                level = LogLevel.ALL
//            }
//            this@createClient.install(WebSockets)
//        }
//
//        var accessToken = client.post("login") {
//            contentType(ContentType.Application.Json)
//            setBody("""{"username": "testUser", "password": "testPassword"}""")
//        }.let {
//            if (it.status == HttpStatusCode.OK)
//            Json.decodeFromString<JsonObject>(it.bodyAsText())
//                .getOrDefault("access", null)?.jsonPrimitive?.content ?: throw Exception("!")
//            else ""
//        }
//        if (accessToken == "") {
//            accessToken = client.post("register") {
//                contentType(ContentType.Application.Json)
//                setBody("""{"username": "testUser", "password": "testPassword"}""")
//            }.let {
//                if (it.status == HttpStatusCode.OK)
//                    Json.decodeFromString<JsonObject>(it.bodyAsText()).getOrDefault("access", null)?.jsonPrimitive?.content ?: throw Exception("Couldn't acquire token")
//                else
//                    throw Exception("Couldn't acquire token")
//            }
//        }
//        return client to accessToken
//    }
//
////    private suspend fun awaitTopicValue(block: () -> String): String {
////        var i = 0
////        while (block() == "" || i > 20) {
////            delay(100)
////            i++
////        }
////        return block()
////    }
//
//    private var id = 1L
//    private fun randomRoom(vol: Float): RoomDto {
//        id++
//        return RoomDto(
//            id = 0,
//            name = "room$id",
//            volume = vol,
//            color = "#" + listOf("FF", "AA", "BB", "CC").shuffled().joinToString(""),
//            scriptCode = ""
//        )
//    }
//
//    private fun HttpRequestBuilder.appJson() {
//        contentType(ContentType.Application.Json)
//    }
//
//    private suspend fun postRandomRoom(client: HttpClient, accessToken: String, vol: Float): RoomDto {
//        val room = randomRoom(vol)
//        client.post("rooms") {
//            header(HttpHeaders.Authorization, "Bearer $accessToken")
//            contentType(ContentType.Application.Json)
//            setBody(room)
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//            return room.copy(id = body<RoomDto>().id)
//        }
//    }
//}