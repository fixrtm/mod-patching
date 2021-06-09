package com.anatawa12.modPatching

import org.gradle.api.Named

interface ModPatch : Named {
    val mod: DownloadingMod

    /**
     * name of files on source tree
     */
    var sourceTreeName: String

    /**
     * name of files on source tree
     */
    var onRepo: OnRepoPatchSource

    /**
     * name of files on source tree
     */
    var onVCS: OnVCSPatchSource
}
