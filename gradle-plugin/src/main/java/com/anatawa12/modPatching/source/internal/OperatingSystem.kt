package com.anatawa12.modPatching.source.internal

enum class OperatingSystem(val extension: String) {
    WINDOWS(".exe"),
    LINUX(""),
    MACOS(""),
    ;

    companion object {
        val current by lazy {
            val osName = System.getProperty("os.name")
            if (osName.startsWith("Windows")) {
                WINDOWS
            } else if (osName.startsWith("Linux")) {
                LINUX
            } else if (osName.startsWith("Mac OS X")) {
                MACOS
            } else if (osName.startsWith("Darwin")) {
                MACOS
            } else {
                null
            }
        }
    }
}
