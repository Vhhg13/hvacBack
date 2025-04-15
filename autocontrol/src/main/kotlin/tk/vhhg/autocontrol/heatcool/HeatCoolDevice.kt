package tk.vhhg.autocontrol.heatcool

data class HeatCoolDevice(
    val topic: String,
    val type: String,
    val maxPower: Double
)
