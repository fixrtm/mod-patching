package com.anatawa12.modPatching.source.internal

enum class Architecture {
    // x64, x86_64, AMD64, Intel 64
    X64,

    // x86, IA-32, i686
    X86,

    // aarch64, armv8
    ARM64,

    // armv7hf
    ARM32,
    ;

    companion object {
        val current: Architecture? by lazy {
            val osArch = System.getProperty("os.arch")

            when {
                osArch.startsWith("armv8") -> {
                    ARM64
                }
                osArch.startsWith("arm") -> {
                    if (osArch.contains("64")) {
                        ARM64
                    } else {
                        ARM32
                    }
                }
                osArch.startsWith("aarch64") -> {
                    ARM64
                }
                osArch.contains("64") -> {
                    X64
                }
                else -> {
                    X86
                }
            }
        }
    }
}
