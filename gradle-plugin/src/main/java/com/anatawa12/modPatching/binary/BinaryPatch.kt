package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.common.DownloadingMod
import org.gradle.api.Named

interface BinaryPatch : Named {
    val mod: DownloadingMod
}
