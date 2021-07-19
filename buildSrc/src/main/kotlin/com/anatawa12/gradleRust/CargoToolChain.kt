package com.anatawa12.gradleRust

import java.io.File
import java.io.IOException
import java.io.Serializable

data class CargoToolChain(
    val cargo: String,
    val rustc: String,
) : Serializable {
    private fun checkExists(): Boolean = try {
        ProcessBuilder(cargo, "--version").start().waitFor() == 0
                && ProcessBuilder(rustc, "--version").start().waitFor() == 0
    } catch (ignored: IOException) {
        false
    }

    /**
     * (target_triple, crate_type) -> (prefix, suffix)
     */
    @Transient
    private val destinationFileType = mutableMapOf<Pair<String, String>, Pair<String, String>>()

    fun getDestinationFileType(triple: String, type: String): Pair<String, String> {
        destinationFileType[triple to type]?.let { return it }
        val archiveName = ProcessBuilder()
            .apply {
                val args = mutableListOf(rustc, "-")
                args.addAll(listOf("--crate-name", "___"))
                args.addAll(listOf("--print", "file-names"))
                args.addAll(listOf("--crate-type", type))
                args.addAll(listOf("--target", triple))
                command(args)
            }
            .start()
            .let {
                it.outputStream.close()
                check(it.waitFor() == 0) {
                    val message = "unsupported target or type: target $triple, type $type"
                    message + "\n" + it.errorStream.reader().readText()
                }
                it.inputStream.reader().readText()
            }
            .lines().first()
        destinationFileType[triple to type]?.let { return it }
        val pair = archiveName.substringBefore("___") to archiveName.substringAfter("___")
        destinationFileType[triple to type] = pair
        return pair
    }

    private val defaultTarget = lazy {
        ProcessBuilder()
            .command(listOf(rustc, "--version", "--verbose"))
            .start()
            .let {
                it.outputStream.close()
                check(it.waitFor() == 0) {
                    "rustc --version --verbose exited with non-zero value\n" +
                            it.errorStream.reader().readText()
                }
                it
            }.inputStream
            .reader()
            .buffered()
            .useLines { lines ->
                lines.filter { it.contains("host") }
                    .map { it.substringAfter(':').trim() }
                    .first()
            }
    }

    fun getDefaultTarget(): String = defaultTarget.value

    companion object {
        val default: CargoToolChain? = selectFrom(
            CargoToolChain("cargo", "rustc"),
            kotlin.run {
                val cargoHome = System.getenv("CARGO_HOME")?.let(::File)
                    ?: File(System.getProperty("user.home")).resolve(".cargo")
                CargoToolChain("$cargoHome/bin/cargo", "$cargoHome/bin/rustc")
            },
        )
        val cross: CargoToolChain? = selectFrom(
            CargoToolChain("cross", "rustc"),
            kotlin.run {
                val cargoHome = System.getenv("CARGO_HOME")?.let(::File)
                    ?: File(System.getProperty("user.home")).resolve(".cargo")
                CargoToolChain("$cargoHome/bin/cross", "$cargoHome/bin/rustc")
            },
        )

        private fun selectFrom(vararg cargoToolChains: CargoToolChain): CargoToolChain? {
            return cargoToolChains.firstOrNull { it.checkExists() }
        }
    }
}
