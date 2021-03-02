package com.anatawa12.modPatching

import com.anatawa12.modPatching.internal.*
import com.anatawa12.modPatching.internal.CommonConstants.PATCHING_DIR_NAME
import com.anatawa12.modPatching.internal.CommonConstants.SCRIPTING_DIR_NAME
import com.anatawa12.modPatching.internal.Constants.COPY_MODS_INTO_MODS_DIR
import com.anatawa12.modPatching.internal.Constants.DECOMPILE_MODS
import com.anatawa12.modPatching.internal.Constants.DOWNLOAD_MODS
import com.anatawa12.modPatching.internal.Constants.GENERATE_UNMODIFIEDS
import com.cloudbees.diff.Diff
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import net.minecraftforge.gradle.util.patching.ContextualPatch
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.internal.classpath.Instrumented
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.util.JarUtil

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

        val downloadMods = project.tasks.create(DOWNLOAD_MODS)
        val decompileMods = project.tasks.create(DECOMPILE_MODS)
        val generateUnmodifieds = project.tasks.create(GENERATE_UNMODIFIEDS)

        val classpath = ClassPathGetter.getOf(
            ModPatchingPlugin::class.java,
            Unit::class.java,
            Diff::class.java,
            IOUtils::class.java,
            JarUtil::class.java,
            ContextualPatch::class.java,
        )
        StartScriptGenerator.generateScript(
            directory = project.projectDir,
            scriptName = "pm.add-modify",
            classpath = classpath,
            mainClassName = "com.anatawa12.modPatching.internalTools.AddModifyFiles",
        )
        StartScriptGenerator.generateScript(
            directory = project.projectDir,
            scriptName = "pm.create-diff",
            classpath = classpath,
            mainClassName = "com.anatawa12.modPatching.internalTools.CreateDiff",
        )
        StartScriptGenerator.generateScript(
            directory = project.projectDir,
            scriptName = "pm.apply-patches",
            classpath = classpath,
            mainClassName = "com.anatawa12.modPatching.internalTools.ApplyPatch",
        )

        val copyModsIntoModsDir = project.tasks.create(COPY_MODS_INTO_MODS_DIR)
        project.tasks.findByName("runClient")?.dependsOn(copyModsIntoModsDir)
        project.tasks.findByName("runServer")?.dependsOn(copyModsIntoModsDir)

        val prepareMods = project.tasks.create(Constants.PREPARE_MODS)
        val preparePatchingEnvironment: Task by project.tasks.creating {
            dependsOn(prepareMods)
            dependsOn(generateUnmodifieds)
        }
        project.tasks.getByName("setupCiWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDecompWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDevWorkspace").dependsOn(preparePatchingEnvironment)

        project.afterEvaluate {
            val patchingModsDir = project.projectDir.resolve(PATCHING_DIR_NAME)
            patchingModsDir.mkdirs()
            patchingModsDir.resolve(".gitkeep").writeText("")
            val logger = project.logger
            if (!projectDir.resolve(".gitignore").readTextOr("").lineSequence().any { it == "pm.*" || it == "/pm.*" }) {
                logger.warn("pm.* is not git ignored! generated patching mod utility should be ignored because " +
                        "they're environment dependent.")
            }
        }
    }
}
