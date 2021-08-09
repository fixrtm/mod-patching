rootProject.name = "mod-patching"
include("gradle-plugin-separated")
include("gradle-plugin")
include("resources-dev-lib")

val cliToolEnabled = when (System.getenv("ENABLE_CLI_TOOL")?.toLowerCase()) {
    null -> exec {
        commandLine("cargo", "--version")
        isIgnoreExitValue = true
        standardOutput = DropOutputStream
        errorOutput = DropOutputStream
    }.exitValue == 0
    "0", "false" -> false
    else -> true
}

// if installed, add cli-tool project (cargo wrapper project)
if (cliToolEnabled) {
    include("cli-tool")
}

object DropOutputStream : java.io.OutputStream() {
    override fun write(b: Int) {
    }

    override fun write(b: ByteArray) {
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
    }
}
