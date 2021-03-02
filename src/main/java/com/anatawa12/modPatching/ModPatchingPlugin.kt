package com.anatawa12.modPatching

import com.anatawa12.modPatching.internal.*
import com.anatawa12.modPatching.internal.Constants.CHECK_SIGNATURE
import com.anatawa12.modPatching.internal.Constants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.internal.Constants.COPY_MODS_INTO_MODS_DIR
import com.anatawa12.modPatching.internal.Constants.DECOMPILE_MODS
import com.anatawa12.modPatching.internal.Constants.DOWNLOAD_MODS
import com.anatawa12.modPatching.internal.Constants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.internal.Constants.GENERATE_UNMODIFIEDS
import com.anatawa12.modPatching.internal.Constants.REGENERATE_JAR
import com.anatawa12.modPatching.internal.Constants.RENAME_SOURCE_NAME
import com.anatawa12.modPatching.internal.Constants.REPROCESS_RESOURCES
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import com.cloudbees.diff.Diff
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import net.minecraftforge.gradle.util.patching.ContextualPatch
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.util.JarUtil

@Suppress("unused")
open class ModPatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val patchingDir = PatchingDir(project.projectDir)

        project.plugins.apply(ForgePlugin::class.java)
        val mods = ModsExtension(project)
        project.extensions.add(ModsContainer::class.java, "mods", mods)
        mods.all { (this as? AbstractDownloadingMod)?.onAdd() }

        val patches = PatchingExtension(project)
        project.extensions.add(ModPatchContainer::class.java, "patching", patches)
        patches.all { (this as? ModPatchImpl)?.onAdd(patchingDir) }

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
            group = "patching"
            dependsOn(prepareMods)
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
            var inJarSpec: CopySpec by this.extra
            destinationDir = Util.getBuildPath(project, "resources")
            from (project.provider { project.zipTree(jarTask.archiveFile) }) {
                inJarSpec = this
            }
        }
        val regenerateJar = project.tasks.create(REGENERATE_JAR, Jar::class) {
            dependsOn(reprocessResources, generateBsdiffPatch)
            destinationDirectory.set(Util.getBuildPath(project, "libs"))
            archiveVersion.set("")
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
