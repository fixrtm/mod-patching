package com.anatawa12.modPatching

import com.anatawa12.modPatching.internal.*
import com.anatawa12.modPatching.internal.Constants.COPY_MODS_INTO_MODS_DIR
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
open class ModPatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ForgePlugin::class.java)
        val mods = ModsExtension(project)
        project.extensions.add(ModsContainer::class.java, "mods", mods)
        mods.all { (this as? AbstractDownloadingMod)?.onAdd() }

        val patches = PatchingExtension(project)
        project.extensions.add(ModPatchContainer::class.java, "patching", patches)
        patches.all { (this as? ModPatchImpl)?.onAdd() }

        val copyModsIntoModsDir = project.tasks.create(COPY_MODS_INTO_MODS_DIR)
        project.tasks.findByName("runClient")?.dependsOn(copyModsIntoModsDir)
        project.tasks.findByName("runServer")?.dependsOn(copyModsIntoModsDir)

        val prepareMods = project.tasks.create(Constants.PREPARE_MODS)
        project.tasks.getByName("setupCiWorkspace").dependsOn(prepareMods)
        project.tasks.getByName("setupDecompWorkspace").dependsOn(prepareMods)
        project.tasks.getByName("setupDevWorkspace").dependsOn(prepareMods)
    }
}
