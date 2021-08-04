package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.common.DownloadingMod
import org.gradle.api.Named

interface ModPatch : Named {
    val mod: DownloadingMod

    /**
     * name of files on source tree
     */
    var sourceTreeName: String
}
