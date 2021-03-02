package com.anatawa12.modPatching.internal

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.*

object StartScriptGenerator {
    val isUnix by lazy { System.getProperty("os.name").toLowerCase().indexOf("win") < 0 }

    fun generateScript(
        directory: File,
        scriptName: String,
        classpath: Set<File>,
        mainClassName: String,
        javaHome: String = System.getProperty("java.home"),
    ) {
        directory.mkdirs()
        val classPath = classpath.joinToString(separator = File.pathSeparator)
        val bashName = directory.resolve(scriptName)
        val batName = directory.resolve("$scriptName.bat")
        if (isUnix) {
            bashName.writeText(generateUnix(javaHome, classPath, mainClassName))
        } else {
            bashName.writeText(generateWindows(javaHome, classPath, mainClassName))
            batName.writeText(generateWindowsBash(javaHome, classPath, mainClassName))
        }
        try {
            Files.setPosixFilePermissions(bashName.toPath(), EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ))
        } catch (ignored: Throwable) {

        }
    }

    private fun generateWindows(
        javaHome: String,
        classpath: String,
        mainClassName: String,
    ): String {
        // @formatter:off
        return """
@rem automatically generated. this is environment dependent.
@rem should not controlled on VCS

$javaHome/bin/java.exe -classpath $classpath $mainClassName %*
"""
        // @formatter:on
    }

    private fun generateWindowsBash(
        javaHome: String,
        classpath: String,
        mainClassName: String,
    ): String {
        // @formatter:off
        return """
#!/usr/bin/env sh
# automatically generated. this is environment dependent.
# should not controlled on VCS

$javaHome/bin/java.exe -classpath $classpath $mainClassName "${'$'}@"
"""
        // @formatter:on
    }

    private fun generateUnix(
        javaHome: String,
        classpath: String,
        mainClassName: String,
    ): String {
        // @formatter:off
        return """
#!/usr/bin/env sh
# automatically generated. this is environment dependent.
# should not controlled on VCS

"$javaHome/bin/java" -classpath $classpath $mainClassName "${'$'}@"
"""
        // @formatter:on
    }
}
