package com.anatawa12.modPatching.resourcesDev.internal

import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.resourcesDev.ResourcesConfig
import com.anatawa12.modPatching.resourcesDev.ResourcesContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class ResourcesDevExtension(private val project: Project) :
    ResourcesContainer,
    NamedDomainObjectContainer<ResourcesConfig> by project.container(ResourcesConfig::class.java) {
    override val forgeFmlCoreModClassName: String
        get() = "com.anatawa12.modPatching.resourcesDev.lib.core.ForgeFmlCoreMod"
    override val cpwFmlCoreModClassName: String
        get() = "com.anatawa12.modPatching.resourcesDev.lib.core.CpwFmlCoreMod"

    override fun ofMod(mod: DownloadingMod, block: Action<ResourcesConfig>): ResourcesConfig {
        require(mod is AbstractDownloadingMod) { "unsupported DownloadingMod: $mod" }
        return ResourcesConfigImpl(mod).apply(block::execute).also(this::add)
    }
}
