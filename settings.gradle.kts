rootProject.name = "mod-patching"
include("gradle-plugin-separated")
include("gradle-plugin")

// check if cargo is installed, this means rust is installed or not
val exitValue = exec {
    commandLine("cargo", "--version")
    isIgnoreExitValue = true
    standardOutput = DropOutputStream
    errorOutput = DropOutputStream
}.exitValue

// if installed, add cli-tool project (cargo wrapper project)
if (exitValue == 0) {
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
