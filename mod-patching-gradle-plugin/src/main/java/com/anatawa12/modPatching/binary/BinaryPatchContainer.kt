package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.common.DownloadingMod
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.util.ConfigureUtil

interface BinaryPatchContainer : NamedDomainObjectCollection<BinaryPatch> {
    var bsdiffPrefix: String
    var sourceNameSuffix: String

    fun patch(mod: DownloadingMod, block: Action<BinaryPatch>): BinaryPatch
    fun patch(mod: DownloadingMod, block: Closure<*>): BinaryPatch = patch(mod, ConfigureUtil.configureUsing(block))
    fun patch(mod: DownloadingMod): BinaryPatch = patch(mod) {}
}
