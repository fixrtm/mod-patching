package com.anatawa12.modPatching.resourcesDev

import com.anatawa12.modPatching.common.DownloadingMod
import org.gradle.api.Named

interface ResourcesConfig : Named {
    val mod: DownloadingMod
}
