package tk.vhhg.autocontrol.heatcool

import org.apache.commons.math3.stat.regression.SimpleRegression

class HeatCoolTask(
    val startTemp: Double,
    val target: Double,
    val deadline: Long?,
    val devices: List<HeatCoolDevice>,
    val volume: Float
) {
    companion object {
        const val HEAT_CAPACITY = 1000.0
        const val DENSITY = 1.2
    }

    private fun E(T: Double): Double = T * HEAT_CAPACITY * DENSITY * volume

    var prevTime = System.currentTimeMillis()
    var prevTemp: Double = startTemp

    val maxPowerArray = DoubleArray(devices.size) { if (devices[it].type == "heat") devices[it].maxPower else 0.0 }
    val minPowerArray = DoubleArray(devices.size) { if (devices[it].type == "cond") devices[it].maxPower else 0.0 }
    val prevResult = DoubleArray(devices.size) { 0.0 }
    val regression = SimpleRegression(true)

    fun feed(temp: Double) = if (target >= startTemp) {
        feedPositive(temp)
    } else {
        feedNegative(temp)
    }

    fun feedNegative(temp: Double): DoubleArray {
        if (temp <= target || (deadline != null && System.currentTimeMillis() > deadline)) {
            val reaction = regression.applyTo(temp)

            val timeLeft = 1
            val energyLeft = E(temp-target)
            val slope = energyLeft / timeLeft

            var neededPower = reaction + slope
            if (!neededPower.isFinite() || neededPower < 0.0) neededPower = 0.0
            println("$reaction + $slope")

            for (i in devices.indices) {
                if (devices[i].type != "cond") {
                    prevResult[i] = 0.0
                    continue
                }
                val maxPower = devices[i].maxPower
                val valueForBroker = minOf(maxPower, neededPower)
                neededPower = maxOf(neededPower - maxPower, 0.0)
                prevResult[i] = valueForBroker
            }
            return prevResult
        }

        val curTime = System.currentTimeMillis()
        val deltaTime = curTime - prevTime
        val deltaE = E(temp) - E(prevTemp)


        val prevSum = -prevResult.sum()
        val latestReactionPower = prevSum - deltaE*1000.0/deltaTime
        if (temp != prevTemp)
            regression.addObservation(doubleArrayOf(prevTemp), latestReactionPower)
        println("$prevTemp reaction $latestReactionPower")

        if (temp > (startTemp + target)/2 || deadline == null) {
            minPowerArray.copyInto(prevResult)
        } else {
            val reaction = regression.applyTo(temp)

            val timeLeft = (deadline - curTime)/1000
            val energyLeft = -E(target - temp)
            val slope = energyLeft / timeLeft

            var neededPower = reaction + slope
            if (!neededPower.isFinite() || neededPower < 0.0) neededPower = 0.0
            println("$reaction + $slope")

            for (i in devices.indices) {
                if (devices[i].type != "cond") {
                    prevResult[i] = 0.0
                    continue
                }
                val maxPower = devices[i].maxPower
                val valueForBroker = minOf(maxPower, neededPower)
                neededPower = maxOf(neededPower - maxPower, 0.0)
                prevResult[i] = valueForBroker
            }
        }
        prevTime = curTime
        prevTemp = temp
        return prevResult
    }

    fun feedPositive(temp: Double): DoubleArray {
        if (temp >= target || (deadline != null && System.currentTimeMillis() > deadline)) {
            val reaction = regression.applyTo(temp)

            val timeLeft = 1
            val energyLeft = E(target - temp)
            val slope = energyLeft / timeLeft

            var neededPower = reaction + slope
            if (!neededPower.isFinite() || neededPower < 0.0) neededPower = 0.0

            for (i in devices.indices) {
                if (devices[i].type != "heat") {
                    prevResult[i] = 0.0
                    continue
                }
                val maxPower = devices[i].maxPower
                val valueForBroker = minOf(maxPower, neededPower)
                neededPower = maxOf(neededPower - maxPower, 0.0)
                prevResult[i] = valueForBroker
            }
            return prevResult
        }

        val curTime = System.currentTimeMillis()
        val deltaTime = curTime - prevTime
        val deltaE = E(temp) - E(prevTemp)


        val prevSum = prevResult.sum()
        val latestReactionPower = prevSum - deltaE*1000.0/deltaTime
        if (temp < prevTemp)
            regression.addObservation(doubleArrayOf(prevTemp), latestReactionPower)
        println("observed prevSum=${prevSum} deltaE=${deltaE} deltaTime=${deltaTime} temp=$temp prevTemp=$prevTemp reac=$latestReactionPower")
        println("[$prevTemp, $latestReactionPower]")

        if (temp < (startTemp + target)/2 || deadline == null) {
            maxPowerArray.copyInto(prevResult)
        } else {
            val reaction = regression.applyTo(temp)

            val timeLeft = (deadline - curTime)/1000
            val energyLeft = E(target - temp)
            val slope = energyLeft / timeLeft

            println("$energyLeft/$timeLeft")
            println("neededPower = $reaction + $slope")
            var neededPower = reaction + slope
            if (!neededPower.isFinite() || neededPower < 0.0) neededPower = 0.0

            for (i in devices.indices) {
                if (devices[i].type != "heat") {
                    prevResult[i] = 0.0
                    continue
                }
                val maxPower = devices[i].maxPower
                val valueForBroker = minOf(maxPower, neededPower)
                neededPower = maxOf(neededPower - maxPower, 0.0)
                prevResult[i] = valueForBroker
            }
        }
        prevTime = curTime
        prevTemp = temp
        return prevResult
    }

    private fun SimpleRegression.applyTo(temp: Double) =
        (intercept + slope * temp).let { if (it.isFinite()) it else 0.0 }
}