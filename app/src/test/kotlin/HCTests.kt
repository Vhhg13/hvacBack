//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import org.eclipse.paho.client.mqttv3.MqttClient
//import tk.vhhg.autocontrol.Broker
//import tk.vhhg.autocontrol.heatcool.HeatCoolDevice
//import tk.vhhg.autocontrol.heatcool.HeaterCooler
//import tk.vhhg.imitation.ImitatedRoomDto
//import tk.vhhg.imitation.Imitator
//import java.time.Instant
//import java.util.concurrent.atomic.AtomicLong
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class HCTests {
//    @Test
//    fun testRoot() = runBlocking {
//        val brokerUrl = "tcp://0.0.0.0:1883"
//        val imitator = Imitator(brokerUrl, 1000)
//        val start = 18F
//        //val broker = Broker.instance("tcp://0.0.0.0:1883")
//        val mqttClient = MqttClient(brokerUrl, "testclient")
//        mqttClient.publish("t100", "".toByteArray(), 2, false)
//        mqttClient.publish("c100", "".toByteArray(), 2, false)
//        mqttClient.publish("h100", "".toByteArray(), 2, false)
//        delay(2000)
//        val volume = 20F
//        imitator.imitate(
//            ImitatedRoomDto(
//                id = 100,
//                heaters = "h100",
//                coolers = "c100",
//                volume = volume,
//                out = start,
//                k = 0.000001F,
//                thermostat = "t100",
//            )
//        )
//
//
//        val heaterCooler = HeaterCooler(mqttClient)
//        val target = 18.5F
//        val maxHeat = 1000.0
//        val maxCold = 900.0
//        val time = 5 * 60L
//        val deadline = Instant.now().plusSeconds(time).toEpochMilli()
//        heaterCooler.start(
//            100, volume, target, deadline, listOf(
//                HeatCoolDevice(topic = "t100", type = "temp", maxPower = 0.0),
//                HeatCoolDevice(topic = "c100", type = "cond", maxPower = maxCold),
//                HeatCoolDevice(topic = "h100", type = "heat", maxPower = maxHeat),
//            )
//        )
//        val heat = AtomicLong()
//        val cool = AtomicLong()
//        mqttClient.subscribe("h100") { topic, message ->
//            val value = String(message.payload).toDouble()
//            heat.set(value.toBits())
//        }
//        mqttClient.subscribe("c100") { topic, message ->
//            val value = String(message.payload).toDouble()
//            cool.set(value.toBits())
//        }
//
//        //val temperature = broker.subscribe("t100")
//        mqttClient.subscribe("t100") { topic, message ->
//            val temp = String(message.payload).toDouble()
//            val heatValue = Double.fromBits(heat.get())
//            val coolValue = Double.fromBits(cool.get())
//            println("t=$temp h=$heatValue c=$coolValue")
//            println("time left ${deadline - Instant.now().toEpochMilli()}")
//            if (temp < (target + start) / 2) {
//                assertEquals(maxHeat, heatValue)
//            } else {
//                assertTrue { (heatValue) < maxHeat }
//            }
//        }
////        val heat = broker.subscribe("h100")
////        val cool = broker.subscribe("c100")
//        //delay(1000)
////        while (temperature.value == "") {
////            println("T")
////            delay(100)
////        }
////        while (heat.value == "") {
////            println("H")
////            delay(100)
////        }
////        while (cool.value == "") {
////            println("C")
////            delay(100)
////        }
////        temperature.collect { t: String ->
////            if (t.isNotEmpty()) {
////                val temp = t.toFloat()
////                println("t=$temp h=${heat.value} c=${cool.value}")
////                println("time left ${deadline - Instant.now().toEpochMilli()}")
////                if (temp < (target + start) / 2) {
////                    assertEquals(maxHeat, heat.value.toDoubleOrNull() ?: maxHeat)
////                } else {
////                    assertTrue { (heat.value.toDoubleOrNull() ?: -1.0) < maxHeat }
////                }
////            } else {
////                println("emptry")
////            }
////        }
//
//    }.let{}
//}