package com.anatawa12.modPatching.common

import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.common.internal.CommonConstants.DOWNLOAD_MODS
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_MODS
import com.anatawa12.modPatching.common.internal.ModsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
open class ModPatchingCommonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val mods = ModsExtension(project)
        project.extensions.add(ModsContainer::class.java, "mods", mods)
        mods.all { (this as? AbstractDownloadingMod)?.onAdd() }

        // phase task
        val downloadMods = project.tasks.create(DOWNLOAD_MODS)
        val prepareMods = project.tasks.create(PREPARE_MODS)
        prepareMods.dependsOn(downloadMods)
    }
}
