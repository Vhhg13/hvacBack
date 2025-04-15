package tk.vhhg.autocontrol

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
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
//        val stateFlow = callbackFlow {
//
//            awaitClose {
//                println("unsubscribed")
//                mqttClient.unsubscribe(topic)
//            }
//        }.stateIn(GlobalScope, SharingStarted.Eagerly, "")
        map[topic] = stateFlow

        return stateFlow
    }

//    fun subscribe(topic: String): Flow<String> {
//        return callbackFlow {
//            mqttClient.subscribe(topic, 2) { topic: String, message ->
//                val msg = String(message.payload)
//                trySend(msg)
//                println("broker received $msg")
//            }
//            awaitClose {
//                println("unsubscribed")
//                mqttClient.unsubscribe(topic)
//            }
//        }
//    }

    fun unsubscribe(topic: String) {
        mqttClient.unsubscribe(topic)
        map.remove(topic)
    }

    operator fun get(topic: String): String = subscribe(topic).value

    operator fun set(topic: String, value: String) {
        mqttClient.publish(topic, value.toByteArray(), 2, false)
    }
}