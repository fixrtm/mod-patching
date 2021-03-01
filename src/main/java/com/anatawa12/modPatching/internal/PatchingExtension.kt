package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.DownloadingMod
import com.anatawa12.modPatching.ModPatch
import com.anatawa12.modPatching.ModPatchContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project

class PatchingExtension(private val project: Project) :
    ModPatchContainer,
    NamedDomainObjectCollection<ModPatch> by project.container(ModPatch::class.java)
{
    override fun patch(mod: DownloadingMod, block: Action<ModPatch>): ModPatch {
        require(mod is AbstractDownloadingMod) { "unsupported DownloadingMod: $mod" }
        return ModPatchImpl(mod)
            .apply(block::execute)
            .apply { freeze() }
            .also { add(it) }
    }
}
