package tk.vhhg.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.vhhg.table.Room
import tk.vhhg.table.Users

class PushNotificator(brokerAddress: String) {

    val mqttClient = MqttClient(brokerAddress, "pushnotificator").apply {
        connect()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun sendNotificationToTheOwnerOfRoom(roomId: Long, message: String) {
        val tokenAndRoomName = newSuspendedTransaction {
            Room.join(Users, JoinType.LEFT, Room.ownerId, Users.id).select(Room.name, Users.pushToken).where {
                Room.id eq roomId
            }.singleOrNull()
        }
        println("token ${tokenAndRoomName?.get(Users.pushToken)}")
        tokenAndRoomName?.let {
            FirebaseMessaging.getInstance().send(
                Message.builder()
                    .setNotification(Notification.builder().setBody(message).setTitle(it[Room.name]).build())
                    .setToken(it[Users.pushToken])
                    .build()
            )
        }
    }

    init {
        println("subscribing")
        mqttClient.subscribe("notifications/+", 2) { topic, message: MqttMessage ->
            println("subscribed $topic")
            val roomId = topic.substringAfter('/').toLongOrNull() ?: return@subscribe
            coroutineScope.launch { sendNotificationToTheOwnerOfRoom(roomId, String(message.payload)) }
        }
    }
}