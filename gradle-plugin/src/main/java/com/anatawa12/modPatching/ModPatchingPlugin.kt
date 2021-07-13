package com.anatawa12.modPatching

import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_MODS
import com.anatawa12.modPatching.internal.ClassPathGetter
import com.anatawa12.modPatching.internal.Constants.CHECK_SIGNATURE
import com.anatawa12.modPatching.internal.Constants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.internal.Constants.DECOMPILE_MODS
import com.anatawa12.modPatching.internal.Constants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.internal.Constants.GENERATE_UNMODIFIEDS
import com.anatawa12.modPatching.internal.Constants.REGENERATE_JAR
import com.anatawa12.modPatching.internal.Constants.RENAME_SOURCE_NAME
import com.anatawa12.modPatching.internal.Constants.REPROCESS_RESOURCES
import com.anatawa12.modPatching.internal.ModPatchImpl
import com.anatawa12.modPatching.internal.PatchingExtension
import com.anatawa12.modPatching.internal.StartScriptGenerator
import com.anatawa12.modPatching.internal.Util
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import com.anatawa12.modPatching.internal.readTextOr
import com.cloudbees.diff.Diff
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import net.minecraftforge.gradle.util.patching.ContextualPatch
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getValue
import org.gradle.util.JarUtil

@Suppress("unused")
open class ModPatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ModPatchingCommonPlugin::class)
        project.plugins.apply(ForgePlugin::class.java)

        val patchingDir = PatchingDir.on(project.projectDir)

        val patches = PatchingExtension(project)
        project.extensions.add(ModPatchContainer::class.java, "patching", patches)
        patches.all { (this as? ModPatchImpl)?.onAdd(patchingDir) }

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


        val prepareMods = project.tasks.create(PREPARE_MODS)
        val preparePatchingEnvironment: Task by project.tasks.creating {
            group = "patching"
            dependsOn(prepareMods)
            dependsOn(decompileMods)
            dependsOn(generateUnmodifieds)
        }
        project.tasks.getByName("setupCiWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDecompWorkspace").dependsOn(preparePatchingEnvironment)
        project.tasks.getByName("setupDevWorkspace").dependsOn(preparePatchingEnvironment)

        generateBuildProcess(project, patches)

        project.afterEvaluate {
            val logger = project.logger
            if (!projectDir.resolve(".gitignore").readTextOr("").lineSequence().any { it == "pm.*" || it == "/pm.*" }) {
                logger.warn("pm.* is not git ignored! generated patching mod utility should be ignored because " +
                        "they're environment dependent.")
            }
        }
    }

    fun generateBuildProcess(project: Project, patches: PatchingExtension) {
        val jarTask = project.tasks.getByName("jar", Jar::class)
        val copyModifiedClasses = project.tasks.create(COPY_MODIFIED_CLASSES, Copy::class) {
            dependsOn("reobfJar")
            into { Util.getBuildPath(project, "modified") }
        }
        val checkSignature = project.tasks.create(CHECK_SIGNATURE, CheckSignatureModification::class) {
            dependsOn(copyModifiedClasses)
            modifiedClasses.add(project.fileTree(Util.getBuildPath(project, "modified")))
        }
        val renameSourceName = project.tasks.create(RENAME_SOURCE_NAME, RenameSourceName::class) {
            dependsOn(copyModifiedClasses)
            classesDir.set(Util.getBuildPath(project, "modified"))
            suffix.set(project.provider { patches.sourceNameSuffix })
        }
        val generateBsdiffPatch = project.tasks.create(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class) {
            dependsOn(renameSourceName)
            newFiles.add(project.fileTree(Util.getBuildPath(project, "modified")))
            patchPrefix.set(project.provider { patches.bsdiffPrefix })
            outTo.set(Util.getBuildPath(project, "patches"))
        }
        val reprocessResources = project.tasks.create(REPROCESS_RESOURCES, Copy::class) {
            dependsOn("reobfJar")
            val copyTask = this
            destinationDir = Util.getBuildPath(project, "resources")
            from (project.provider { project.zipTree(jarTask.archiveFile) }) {
                copyTask.extra["inJarSpec"] = this
            }
            from (project.provider { project.zipTree(jarTask.archiveFile) }) {
                include("META-INF/MANIFEST.MF")
            }
        }
        val regenerateJar = project.tasks.create(REGENERATE_JAR, Zip::class) {
            dependsOn(reprocessResources, generateBsdiffPatch)
            destinationDirectory.set(Util.getBuildPath(project, "libs"))
            archiveVersion.set("")
            archiveExtension.set("jar")
            from(reprocessResources.destinationDir)
            from(generateBsdiffPatch.outTo)
        }
        val copyJar = project.tasks.create("copyJar") {
            dependsOn(regenerateJar)
            doLast {
                regenerateJar.archiveFile.get().asFile.inputStream().use { src ->
                    jarTask.archiveFile.get().asFile.apply { parentFile.mkdirs() }
                        .outputStream()
                        .use { dst -> src.copyTo(dst) }
                }
            }
        }
        project.tasks.getByPath("check").dependsOn(checkSignature)
        project.tasks.getByPath("assemble").dependsOn(copyJar)
    }
}
