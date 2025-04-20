package tk.vhhg.autocontrol.scripting

data class Script(
    val roomId: Long,
    val topics: List<String>,
    val code: String,
) {
    val wrappedCode: String get() = "{ List topics -> $code }"

    override fun toString(): String {
        return "${super.toString()} topics=${topics} wrappedCode=$wrappedCode}"
    }
}