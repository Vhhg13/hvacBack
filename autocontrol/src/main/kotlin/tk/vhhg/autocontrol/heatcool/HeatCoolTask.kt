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
    private var hasTrained = false

    fun feedNegative(temp: Double): DoubleArray {
        if (hasTrained || temp <= target || (deadline != null && System.currentTimeMillis() > deadline)) {
            hasTrained = true
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

        if (temp > (startTemp + target)/2 || deadline == null) {
            println("$temp $startTemp $target")
            minPowerArray.copyInto(prevResult)
        } else {
            val reaction = regression.applyTo(temp)

            val timeLeft = (deadline - curTime)/1000
            val energyLeft = -E(target - temp)
            val slope = energyLeft / timeLeft

            var neededPower = reaction + slope
            if (!neededPower.isFinite() || neededPower < 0.0) neededPower = 0.0

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

    private fun regress(temp: Double, timeLeft: Long) {
        val reaction = regression.applyTo(temp)
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
    }

    fun feedPositive(temp: Double): DoubleArray {
        if (hasTrained || temp >= target || (deadline != null && System.currentTimeMillis() > deadline)) {
            hasTrained = true
            regress(temp, 1)
            return prevResult
        }
        val curTime = System.currentTimeMillis()
        val deltaTime = curTime - prevTime
        val deltaE = E(temp) - E(prevTemp)
        val prevSum = prevResult.sum()
        val latestReactionPower = prevSum - deltaE*1000.0/deltaTime
        if (temp < prevTemp)
            regression.addObservation(doubleArrayOf(prevTemp), latestReactionPower)
        if (temp < (startTemp + target)/2 || deadline == null)
            maxPowerArray.copyInto(prevResult)
        else
            regress(temp, (deadline - curTime)/1000)
        prevTime = curTime
        prevTemp = temp
        return prevResult
    }

    private fun SimpleRegression.applyTo(temp: Double) =
        (intercept + slope * temp).let { if (it.isFinite()) it else 0.0 }
}