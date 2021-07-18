package com.anatawa12.modPatching.source.internal

import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.source.ModPatch
import com.anatawa12.modPatching.source.SourcePatchContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project

class SourcePatchingExtension(private val project: Project) :
    SourcePatchContainer,
    NamedDomainObjectCollection<ModPatch> by project.container(ModPatch::class.java) {
    override var bsdiffPrefix: String = ""
    override var sourceNameSuffix: String = ""
    override lateinit var mappingName: String
    override lateinit var mcVersion: String
    override lateinit var forgeFlowerVersion: String
    val mappingChannel get() = mappingName.substringBefore('_')
    val mappingVersion get() = mappingName.substringAfter('-')

    override fun patch(mod: DownloadingMod, block: Action<ModPatch>): ModPatch {
        require(mod is AbstractDownloadingMod) { "unsupported DownloadingMod: $mod" }
        return SourcePatchImpl(mod, this)
            .apply(block::execute)
            .apply { freeze() }
            .also { add(it) }
    }
}
