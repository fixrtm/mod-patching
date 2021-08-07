package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.source.internal.OperatingSystem

enum class InstallingType {
    Symlink,
    Copying,
    ;

    companion object {
        val default by lazy {
            when (OperatingSystem.current) {
                // on posix, it works well
                OperatingSystem.LINUX -> Symlink
                OperatingSystem.MACOS -> Symlink
                // on Windows, symlink doesn't work well
                OperatingSystem.WINDOWS -> Copying
                // symlink may not work well on some platforms like windows
                // so use copy by default.
                null -> Copying
            }
        }
    }
}
