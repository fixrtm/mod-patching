package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.common.DownloadingMod
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.util.ConfigureUtil

interface SourcePatchContainer : NamedDomainObjectCollection<ModPatch> {
    // channel_version
    var mappingName: String
    var mcVersion: String
    var forgeFlowerVersion: String

    fun patch(mod: DownloadingMod, block: Action<ModPatch>): ModPatch
    fun patch(mod: DownloadingMod, block: Closure<*>): ModPatch = patch(mod, ConfigureUtil.configureUsing(block))
    fun patch(mod: DownloadingMod): ModPatch = patch(mod) {}
}
