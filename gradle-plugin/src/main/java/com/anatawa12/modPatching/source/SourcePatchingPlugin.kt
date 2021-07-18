package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_MODS
import com.anatawa12.modPatching.source.internal.PatchingDir
import com.anatawa12.modPatching.source.internal.SourceConstants.DECOMPILE_MODS
import com.anatawa12.modPatching.source.internal.SourceConstants.FORGEFLOWER_CONFIGURATION
import com.anatawa12.modPatching.source.internal.SourceConstants.MAPPING_CONFIGURATION
import com.anatawa12.modPatching.source.internal.SourcePatchImpl
import com.anatawa12.modPatching.source.internal.SourcePatchingExtension
import com.anatawa12.modPatching.source.internal.readTextOr
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue

@Suppress("unused")
open class SourcePatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ModPatchingCommonPlugin::class)
        project.plugins.apply(ForgePlugin::class.java)

        val patchingDir = PatchingDir.on(project.projectDir)

        val patches = SourcePatchingExtension(project)
        project.extensions.add(SourcePatchContainer::class.java, "patching", patches)
        patches.all { (this as? SourcePatchImpl)?.onAdd(patchingDir) }

        project.configurations.maybeCreate(MAPPING_CONFIGURATION)
        project.configurations.maybeCreate(FORGEFLOWER_CONFIGURATION)

        project.dependencies.add(
            MAPPING_CONFIGURATION,
            project.provider {
                "de.oceanlabs.mcp" +
                        ":mcp_${patches.mappingChannel}" +
                        ":${patches.mappingVersion}-${patches.mcVersion}" +
                        "@zip"
            },
        )

        project.dependencies.add(
            FORGEFLOWER_CONFIGURATION,
            project.provider {
                "net.minecraftforge:forgeflower:${patches.forgeFlowerVersion}"
            },
        )

        val decompileMods = project.tasks.create(DECOMPILE_MODS)

        // TODO: install patching-mod

        val prepareMods = project.tasks.getByName(PREPARE_MODS)
        val preparePatchingEnvironment: Task by project.tasks.creating {
            group = "patching"
            dependsOn(prepareMods)
            dependsOn(decompileMods)
        }
        project.tasks.getByName("setupCiWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDecompWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDevWorkspace").dependsOn(preparePatchingEnvironment)

        project.afterEvaluate {
            val logger = project.logger
            if (!projectDir.resolve(".gitignore").readTextOr("").lineSequence().any { it == "pm.*" || it == "/pm.*" }) {
                logger.warn("pm.* is not git ignored! generated patching mod utility should be ignored because " +
                        "they're environment dependent.")
            }
        }
    }
}
