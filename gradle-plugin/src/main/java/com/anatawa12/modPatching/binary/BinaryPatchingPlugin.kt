package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.binary.internal.BinaryConstants.CHECK_SIGNATURE
import com.anatawa12.modPatching.binary.internal.BinaryConstants.COPY_JAR
import com.anatawa12.modPatching.binary.internal.BinaryConstants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.binary.internal.BinaryConstants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.binary.internal.BinaryConstants.LIST_MODIFIED_CLASSES
import com.anatawa12.modPatching.binary.internal.BinaryConstants.REGENERATE_JAR
import com.anatawa12.modPatching.binary.internal.BinaryConstants.RENAME_SOURCE_NAME
import com.anatawa12.modPatching.binary.internal.BinaryPatchImpl
import com.anatawa12.modPatching.binary.internal.BinaryPatchingExtension
import com.anatawa12.modPatching.binary.internal.Util
import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName

@Suppress("unused")
open class BinaryPatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ModPatchingCommonPlugin::class)
        project.plugins.apply(JavaPlugin::class)

        val patches = BinaryPatchingExtension(project)
        project.extensions.add(BinaryPatchContainer::class.java, "binPatching", patches)
        patches.all { (this as? BinaryPatchImpl)?.onAdd() }

        generateBuildProcess(project, patches)
    }

    fun generateBuildProcess(project: Project, patches: BinaryPatchingExtension) {
        val jarTask = project.tasks.getByName("jar", Jar::class)
        //val modifiedClassesPath = Util.getBuildPath(project, "modified")

        val listModifiedClasses = project.tasks.create(LIST_MODIFIED_CLASSES, ListModifiedClasses::class) {
            dependsOn(jarTask)
            newerJar.set(jarTask.archiveFile)
            modifiedInfoDir.set(Util.getBuildPath(project, "modified-infos"))
        }
        val copyModifiedClasses = project.tasks.create(COPY_MODIFIED_CLASSES, Copy::class) {
            dependsOn(jarTask, listModifiedClasses)
            into(Util.getBuildPath(project, "modified"))
            from(jarTask.archiveFile) {
                include { listModifiedClasses.isModified(it.path) }
            }
        }
        val checkSignature = project.tasks.create(CHECK_SIGNATURE, CheckSignatureModification::class) {
            dependsOn(copyModifiedClasses)
            modifiedClasses.add(project.provider { project.fileTree(copyModifiedClasses.destinationDir) })
        }
        val renameSourceName = project.tasks.create(RENAME_SOURCE_NAME, RenameSourceName::class) {
            dependsOn(copyModifiedClasses)
            classesDir.set(project.provider { copyModifiedClasses.destinationDir })
            suffix.set(project.provider { patches.sourceNameSuffix })
        }
        val generateBsdiffPatch = project.tasks.create(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class) {
            dependsOn(copyModifiedClasses, renameSourceName)
            newFiles.add(project.fileTree(copyModifiedClasses.destinationDir))
            patchPrefix.set(project.provider { patches.bsdiffPrefix })
            outTo.set(Util.getBuildPath(project, "patches"))
        }
        val regenerateJar = project.tasks.create(REGENERATE_JAR, Zip::class) {
            dependsOn(copyModifiedClasses, generateBsdiffPatch)
            destinationDirectory.set(Util.getBuildPath(project, "libs"))
            archiveVersion.set("")
            archiveExtension.set("jar")
            from(project.provider { project.zipTree(jarTask.archiveFile) }) {
                // if modified is a class, do not include
                exclude { !listModifiedClasses.isUnmodified(it.path) }
            }
            from(generateBsdiffPatch.outTo)
        }
        val copyJar = project.tasks.create(COPY_JAR) {
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
