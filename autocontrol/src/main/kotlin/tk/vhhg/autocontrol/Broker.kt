package tk.vhhg.autocontrol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.client.mqttv3.MqttClient
import java.util.concurrent.ConcurrentHashMap

class Broker private constructor(url: String) {

    companion object {
        private val instances = mutableMapOf<String, Broker>()
        fun instance(url: String): Broker {
            var broker = instances[url]
            if (broker == null) {
                broker = Broker(url)
                instances[url] = broker
            }
            return broker
        }
    }

    private val mqttClient = MqttClient(url, "broker").apply {
        connect()
    }

    private val map = ConcurrentHashMap<String, StateFlow<String>>() // topic to its current value

    fun subscribe(topic: String): StateFlow<String> {
        map[topic]?.let { return it }
        val stateFlow = MutableStateFlow("")
        mqttClient.subscribe(topic, 2) { topic: String, message ->
            val msg = String(message.payload)
            stateFlow.value = msg
        }
        map[topic] = stateFlow
        return stateFlow
    }

    fun unsubscribe(topic: String) {
        mqttClient.unsubscribe(topic)
        map.remove(topic)
    }

    operator fun get(topic: String): String = subscribe(topic).value

    operator fun set(topic: String, value: String) {
        mqttClient.publish(topic, value.toByteArray(), 2, false)
    }
}