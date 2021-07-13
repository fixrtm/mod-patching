package com.anatawa12.modPatching

import com.anatawa12.modPatching.common.DownloadingMod
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.util.ConfigureUtil

interface ModPatchContainer : NamedDomainObjectCollection<ModPatch> {
    var bsdiffPrefix: String
    var sourceNameSuffix: String

    fun patch(mod: DownloadingMod, block: Action<ModPatch>): ModPatch
    fun patch(mod: DownloadingMod, block: Closure<*>): ModPatch = patch(mod, ConfigureUtil.configureUsing(block))
    fun patch(mod: DownloadingMod): ModPatch = patch(mod) {}
}
