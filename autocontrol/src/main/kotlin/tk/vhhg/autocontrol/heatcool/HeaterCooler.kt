package tk.vhhg.autocontrol.heatcool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.client.mqttv3.MqttClient
import org.jetbrains.exposed.sql.IsNullOp
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andIfNotNull
import tk.vhhg.autocontrol.Broker
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import java.util.concurrent.ConcurrentHashMap

class HeaterCooler(
    private val broker: Broker
//    private val mqttClient: MqttClient
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val tasks = ConcurrentHashMap<Long, Job>()

    fun cancel(roomId: Long) {
        tasks[roomId]?.cancel()
    }

    fun start(
        roomId: Long,
        volume: Float,
        target: Float?,
        deadline: Long?,
        devices: List<HeatCoolDevice>
    ) {
        tasks[roomId]?.cancel()

        val thermostat = devices.getOrNull(0)?.let { if (it.type == "temp") it else null }

        val otherDevices = devices.subList(if (thermostat == null) 0 else 1, devices.size)
        if (otherDevices.isEmpty()) return

        if (target == null) {
            for (device in otherDevices) {
                //mqttClient.publish(device.topic, "0".toByteArray(), 2, false)
                broker[device.topic] = "0"
            }
            return
        }

        if (thermostat == null) return

        val job = coroutineScope.launch {
            val task = HeatCoolTask(
                startTemp = awaitTopicValue { broker[thermostat.topic] }.toDouble(),
                target = target.toDouble(),
                deadline = deadline,
                devices = otherDevices,
                volume = volume
            )
//            mqttClient.subscribe(thermostat.topic) { topic, message ->
//
//            }
            while (true) {
                delay(1000)
                val tempStr = broker[thermostat.topic]
                val powers = task.feed(tempStr.toDouble())
                println("powers ${powers.joinToString(" ")}")
                for (i in powers.indices) {
//                for (i in otherDevices.indices) {
                    //mqttClient.publish(otherDevices[i].topic, powers[i].toString().toByteArray(), 2, false)
                    broker[otherDevices[i].topic] = powers[i].toString()
                }
//                if (powers.all { it == 0.0 }) {
                if (powers.isEmpty()) {
                    cancel()
                    break
                }
            }
        }
        tasks[roomId] = job
    }

    private suspend fun awaitTopicValue(block: () -> String): String {
        while (block() == "") {
            delay(100)
        }
        return block()
    }

}