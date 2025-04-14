//package tk.vhhg.autocontrol.scripting
//
//import groovy.lang.Closure
//import groovy.lang.GroovyShell
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.ensureActive
//import kotlinx.coroutines.launch
//import tk.vhhg.autocontrol.Broker
//import java.util.concurrent.ConcurrentHashMap
//import kotlin.collections.iterator
//import kotlin.random.Random
//import kotlin.random.nextLong
//
//class ScriptExecutor(private val broker: Broker, private val lockedRooms: Map<Long, Boolean>) {
//
//    private val groovyShell = GroovyShell()
//
//    companion object {
//        const val DELAY = 1000L
//    }
//
//    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//    private val scheduledScripts = ConcurrentHashMap<Long, Pair<Script, Job?>>()
//
//    fun updateScript(roomId: Long, code: String) {
//        scheduledScripts[roomId]?.let { (script, job) ->
//            if (script.code != code) {
//                job?.cancel()
//                val newScript = script.copy(code = code)
//                scheduledScripts[roomId] = newScript to getScriptJob(newScript)
//            }
//        } ?: run {
//            scheduledScripts[roomId] = Script(roomId, emptyList(), code) to null
//        }
//    }
//
//    private fun getScriptJob(script: Script): Job {
//        return coroutineScope.launch {
//            script.topics.forEach {
//                broker.subscribe(it)
//            }
//            delay(DELAY + Random.nextLong(0, DELAY))
//            while (true) {
//                delay(DELAY)
//                val topicValues = script.topics.map { topicName ->
//                    broker[topicName]
//                }
//                val results = script.run(topicValues)
//                ensureActive()
//                if (lockedRooms[script.roomId] == true) continue
//                for (i in script.topics.indices) {
//                    results.getOrNull(i)?.let {
//                        //broker[script.topics[i]] = it
//                    }
//                }
//                println("${script.roomId} ${System.currentTimeMillis()} $topicValues -> $results")
//            }
//        }
//    }
//
//    fun schedule(script: Script) {
//        println("script $script")
//        scheduledScripts[script.roomId]?.second?.cancel()
//        val job = getScriptJob(script)
//        scheduledScripts[script.roomId] = script to job
//    }
//
//    fun remove(roomId: Long) {
//        scheduledScripts.remove(roomId)?.second?.cancel()
//    }
//
//    fun Script.run(topicValues: List<String>): List<String?> {
//        if (code == "") return emptyList()
//        val f = groovyShell.evaluate(wrappedCode) as? Closure<*>
//        if (f == null) {
//            scheduledScripts[roomId]?.second?.cancel()
//            return emptyList()
//        }
//        return (f.call(topicValues) as List<*>).map { it?.toString() }
//    }
//
//    fun updateDevices(roomId: Long, topics: List<String>) {
//        println("update $topics")
//        scheduledScripts[roomId]!!.let { (script, job) ->
//            job?.cancel()
//            val newScript = script.copy(topics = topics)
//            scheduledScripts[roomId] = newScript to getScriptJob(newScript)
//        }
//    }
//
//    fun removeTopic(roomId: Long, topic: String) {
//        scheduledScripts[roomId]!!.let { (script, job) ->
//            job?.cancel()
//            val newScript = script.copy(topics = script.topics.filterNot { it == topic })
//            scheduledScripts[roomId] = newScript to getScriptJob(newScript)
//        }
//    }
//
//    fun scheduleAll(q: Map<Pair<Long, String>, List<String?>>) {
//        for ((k, v) in q) {
//            if (v.first() == null)
//                schedule(Script(k.first, emptyList(), k.second))
//            else
//                schedule(Script(k.first, v.map(::checkNotNull), k.second))
//        }
//    }
//}