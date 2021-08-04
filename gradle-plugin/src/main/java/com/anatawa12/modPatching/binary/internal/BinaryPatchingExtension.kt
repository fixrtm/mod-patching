package com.anatawa12.modPatching.binary.internal

import com.anatawa12.modPatching.binary.BinaryPatch
import com.anatawa12.modPatching.binary.BinaryPatchContainer
import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project

class BinaryPatchingExtension(private val project: Project) :
    BinaryPatchContainer,
    NamedDomainObjectCollection<BinaryPatch> by project.container(BinaryPatch::class.java) {
    override var bsdiffPrefix: String = ""
    override var sourceNameSuffix: String = ""

    override fun patch(mod: DownloadingMod, block: Action<BinaryPatch>): BinaryPatch {
        require(mod is AbstractDownloadingMod) { "unsupported DownloadingMod: $mod" }
        return BinaryPatchImpl(mod)
            .apply(block::execute)
            .apply { freeze() }
            .also { add(it) }
    }
}
