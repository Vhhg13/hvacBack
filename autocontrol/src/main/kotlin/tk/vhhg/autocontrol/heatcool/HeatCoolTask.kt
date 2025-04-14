//package tk.vhhg.autocontrol.heatcool
//
//import org.apache.commons.math3.stat.regression.SimpleRegression
//
//class HeatCoolTask(
//    val startTemp: Double,
//    val target: Double,
//    val deadline: Long?,
//    val devices: List<HeatCoolDevice>,
//    val volume: Float
//) {
//    companion object {
//        const val HEAT_CAPACITY = 1000.0
//        const val DENSITY = 1.2
//    }
//
//    private fun E(T: Double): Double = T * HEAT_CAPACITY * DENSITY * volume
//
//    var prevTime = System.currentTimeMillis()
//    var prevTemp: Double = startTemp
//        set(value) {
//            println("prevTemp $value")
//            field = value
//        }
//
//    val maxPowerArray = DoubleArray(devices.size) { if (devices[it].type == "heat") devices[it].maxPower else 0.0 }
//    val prevResult = DoubleArray(devices.size) { 0.0 }
//    val regression = SimpleRegression(true)
//
//    fun feed(temp: Double): DoubleArray {
//        if (temp >= target) return doubleArrayOf()
//        if (deadline == null) return maxPowerArray
//
//        // deadline != null && temp < target
//
//        val curTime = System.currentTimeMillis()
//        val deltaTime = curTime - prevTime
//        val deltaE = E(temp) - E(prevTemp)
//
//
//        val prevSum = prevResult.sum()
//        val latestReactionPower = prevSum - deltaE/deltaTime
//        regression.addObservation(doubleArrayOf(prevTemp), latestReactionPower)
//        println("observed prevSum=${prevSum} deltaE=${deltaE} deltaTime=${deltaTime} prevTemp=$prevTemp reac=$latestReactionPower")
////        regression.apply {
////            println("$intercept + $slope *x")
////        }
//
//        if (temp < (startTemp + target)/2) {
//            maxPowerArray.copyInto(prevResult)
//        } else {
//            val reaction = regression.applyTo(temp)
//
//            val timeLeft = deadline - curTime
//            val energyLeft = E(target) - E(temp)
//            val slope = energyLeft / timeLeft
//
//            var neededPower = reaction + slope
//            println("neededPower $neededPower")
//            for (i in devices.indices) {
//                if (devices[i].type != "heat") {
//                    prevResult[i] = 0.0
//                    continue
//                }
//                val maxPower = devices[i].maxPower
//                val valueForBroker = minOf(maxPower, neededPower)
//                neededPower = maxOf(neededPower - maxPower, 0.0)
//                prevResult[i] = valueForBroker
//            }
//        }
//        prevTime = curTime
//        prevTemp = temp
//        return prevResult
//    }
//
//    private fun SimpleRegression.applyTo(temp: Double) = intercept + slope * temp
//}