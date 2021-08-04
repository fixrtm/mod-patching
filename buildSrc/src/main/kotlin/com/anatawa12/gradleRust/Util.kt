package com.anatawa12.gradleRust

import org.gradle.api.Project
import java.io.ByteArrayOutputStream

fun Project.execWithStdout(vararg args: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        isIgnoreExitValue = false
        standardOutput = stdout
        args(*args)
    }
    return stdout.toString(Charsets.UTF_8.name())
}
