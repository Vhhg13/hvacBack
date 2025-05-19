package tk.vhhg.autocontrol.scripting

data class Script(
    val roomId: Long,
    val topics: List<String>,
    val code: String,
) {
    val wrappedCode: String get() = """
        import org.jetbrains.annotations.NotNull
        import tk.vhhg.autocontrol.scripting.NotifyingScript
        new NotifyingScript(x, y) {
            @Override
            List<Double> run(@NotNull List<Double> devices) {
                $code
            }
        }
        """.trimIndent()

    override fun toString(): String {
        return "${super.toString()} topics=${topics} wrappedCode=$wrappedCode}"
    }
}
