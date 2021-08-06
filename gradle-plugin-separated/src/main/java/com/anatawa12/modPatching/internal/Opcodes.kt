package com.anatawa12.modPatching.internal

import org.objectweb.asm.Opcodes

object ASMVersions {
    private val _latest = kotlin.run {
        val opcodes = Opcodes::class.java
        for (name in arrayOf(
            "ASM9",
            "ASM8",
            "ASM7",
            "ASM6",
            "ASM5",
            "ASM4",
        )) {
            try {
                return@run opcodes.getField(name).getInt(null)
            } catch (_: NoSuchFieldException) {
            }
        }
        //error("current version of ASM doesn't support for version 4..9.")
        null
    }
    val latest get() = _latest ?: error("current version of ASM doesn't support for version 4..9.")

    fun getLatestInRange(since: Int, until: Int) =
        latest.coerceIn(since, until)
}
