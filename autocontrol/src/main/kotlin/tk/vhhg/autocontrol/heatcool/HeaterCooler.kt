package tk.vhhg.autocontrol.heatcool

import kotlinx.coroutines.*
import tk.vhhg.autocontrol.Broker
import java.util.concurrent.ConcurrentHashMap

class HeaterCooler(private val broker: Broker, private val roomsLock: ConcurrentHashMap<Long, Boolean>) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tasks = ConcurrentHashMap<Long, Job>()

    fun cancel(roomId: Long) {
        tasks[roomId]?.cancel()
        unlock(roomId)
    }

    fun start(roomId: Long, volume: Float, target: Float?, deadline: Long?, devices: List<HeatCoolDevice>) {
        lock(roomId)
        tasks[roomId]?.cancel()
        val thermostat = devices.firstOrNull()?.let { if (it.type == "temp") it else null }
        val otherDevices = devices.subList(if (thermostat == null) 0 else 1, devices.size)
        if (otherDevices.isEmpty()) return
        if (target == null) {
            for (device in otherDevices) broker[device.topic] = "0"
            unlock(roomId)
            return
        }
        if (thermostat == null) return
        tasks[roomId] = coroutineScope.launch {
            val task = HeatCoolTask(
                awaitTopicValue { broker[thermostat.topic] }.toDouble(),
                target.toDouble(), deadline, otherDevices, volume)
            while (true) {
                delay(1000)
                val temperatureString = broker[thermostat.topic]
                val powers = task.feed(temperatureString.toDouble())
                for (i in powers.indices)
                    broker[otherDevices[i].topic] = powers[i].toString()
                if (powers.isEmpty()) break
            }
            unlock(roomId)
        }
    }

    private suspend fun awaitTopicValue(block: () -> String): String {
        while (block() == "") delay(100)
        return block()
    }

    private fun lock(roomId: Long) {
        roomsLock[roomId] = true
    }

    private fun unlock(roomId: Long) {
        roomsLock.remove(roomId)
    }

}