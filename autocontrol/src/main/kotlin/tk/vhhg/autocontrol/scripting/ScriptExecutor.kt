package tk.vhhg.autocontrol.scripting

import groovy.lang.Closure
import groovy.lang.GroovyShell
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.transactions.transaction
import tk.vhhg.autocontrol.Broker
import tk.vhhg.table.Device
import tk.vhhg.table.Room
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ScriptExecutor(private val broker: Broker, private val lockedRooms: ConcurrentHashMap<Long, Boolean>) {

    private val groovyShell = GroovyShell()

    companion object {
        const val DELAY = 1000L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scheduledScripts = ConcurrentHashMap<Long, Pair<Script, Job?>>()

    private fun getScriptJob(script: Script): Job {
        return coroutineScope.launch {
            script.topics.forEach {
                broker.subscribe(it)
            }
            delay(DELAY + Random.nextLong(0, DELAY))
            while (true) {
                delay(DELAY)
                val topicValues = script.topics.map { topicName ->
                    broker[topicName]
                }.map { it.toDoubleOrNull() ?: 0.0 }
                val results = script.run(topicValues)
                ensureActive()
                if (lockedRooms[script.roomId] == true) continue
                for (i in script.topics.indices) {
                    results.getOrNull(i)?.let {
                        broker[script.topics[i]] = it
                    }
                }
                println("${script.roomId} ${System.currentTimeMillis()} $topicValues -> $results")
            }
        }
    }

    fun schedule(script: Script) {
        println("script $script")
        scheduledScripts[script.roomId]?.second?.cancel()
        val job = getScriptJob(script)
        scheduledScripts[script.roomId] = script to job
    }

    fun remove(roomId: Long) {
        scheduledScripts.remove(roomId)?.second?.cancel()
    }

    fun Script.run(topicValues: List<Double>): List<String?> {
        if (code.isBlank()) return emptyList()
        val f = groovyShell.evaluate(wrappedCode) as? Closure<*>
        if (f == null) {
            scheduledScripts[roomId]?.second?.cancel()
            return emptyList()
        }
        return (f.call(topicValues) as List<*>).map { it?.toString() }
    }

    fun scheduleAll(q: Map<Pair<Long, String>, List<String?>>) {
        for ((k, v) in q) {
            if (v.first() == null)
                schedule(Script(k.first, emptyList(), k.second))
            else
                schedule(Script(k.first, v.map(::checkNotNull), k.second))
        }
    }

    fun runAll() {
        scheduledScripts.values.forEach {
            it.second?.cancel()
        }
        scheduledScripts.clear()
        transaction {
            println("Running all")
            val q = Device.join(Room, JoinType.FULL, Device.roomId, Room.id)
                .select(Room.id, Room.scriptCode, Device.topic, Device.id)
                .orderBy(Device.id)
                .groupBy(
                    keySelector = {
                        it[Room.id].value to it[Room.scriptCode]
                    },
                    valueTransform = { it.getOrNull(Device.topic) }
                )
            println("scripts in db: $q")
            scheduleAll(q)
        }
    }
}