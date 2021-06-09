package com.anatawa12.modPatching.internalTools

import java.io.File

object GitWrapper {
    fun add(file: File) {
        ProcessBuilder("git", "add", "${file.absoluteFile}")
            .inheritIO()
            .start()
            .waitFor()
    }
}
