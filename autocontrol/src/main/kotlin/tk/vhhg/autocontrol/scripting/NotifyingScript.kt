package tk.vhhg.autocontrol.scripting

import tk.vhhg.autocontrol.Broker

abstract class NotifyingScript(private val roomId: Long,
                               private val broker: Broker) {
    private val notificationsInOneRun = mutableListOf<Boolean>()
    private var notificationIndex = 0
    abstract fun run(topics: List<Double>): List<Number?>
    fun runWithState(topics: List<Double>): List<Double?> {
        notificationIndex = 0
        return run(topics).map { it?.toDouble() }
    }

    fun notify(cond: Boolean, msg: String) {
        if (notificationsInOneRun.size == notificationIndex) notificationsInOneRun.add(false)
        val previousCondValue = notificationsInOneRun[notificationIndex]
        if (cond && previousCondValue == false) broker["notifications/$roomId"] = msg
        notificationsInOneRun[notificationIndex++] = cond
    }
}