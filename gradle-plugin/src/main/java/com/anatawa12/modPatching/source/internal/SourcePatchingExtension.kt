package com.anatawa12.modPatching.source.internal

import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.source.ModPatch
import com.anatawa12.modPatching.source.SourcePatchContainer
import com.anatawa12.modPatching.source.internal.SourceConstants.DECOMPILER_CONFIGURATION
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project

class SourcePatchingExtension(private val project: Project) :
    SourcePatchContainer,
    NamedDomainObjectCollection<ModPatch> by project.container(ModPatch::class.java) {
    override lateinit var mappingName: String
    override lateinit var mcVersion: String
    private var dependencyAdded = false
    override var forgeFlowerVersion: String = ""
        set(value) {
            if (!dependencyAdded) {
                project.logger.warn(
                    "forgeFlowerVersion property is deprecated." +
                            "You should use $DECOMPILER_CONFIGURATION in your dependencies block to make easy for bot " +
                            "to upgrade decompiler automatically."
                )
                dependencyAdded = true
                project.dependencies.add(
                    DECOMPILER_CONFIGURATION,
                    project.provider {
                        "net.minecraftforge:forgeflower:${forgeFlowerVersion}"
                    },
                )
            }
            field = value
        }
    override var autoInstallCli: Boolean = false
    val mappingChannel get() = mappingName.substringBefore('_')
    val mappingVersion get() = mappingName.substringAfter('_')

    override fun patch(mod: DownloadingMod, block: Action<ModPatch>): ModPatch {
        require(mod is AbstractDownloadingMod) { "unsupported DownloadingMod: $mod" }
        return SourcePatchImpl(mod, this)
            .apply(block::execute)
            .apply { freeze() }
            .also { add(it) }
    }
}
