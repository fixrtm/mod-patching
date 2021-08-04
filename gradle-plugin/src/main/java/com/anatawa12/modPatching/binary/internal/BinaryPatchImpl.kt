package com.anatawa12.modPatching.binary.internal

import com.anatawa12.modPatching.binary.BinaryPatch
import com.anatawa12.modPatching.binary.CheckSignatureModification
import com.anatawa12.modPatching.binary.GenerateBsdiffPatch
import com.anatawa12.modPatching.binary.internal.BinaryConstants.CHECK_SIGNATURE
import com.anatawa12.modPatching.binary.internal.BinaryConstants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.binary.internal.BinaryConstants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.common.internal.Delegates
import com.anatawa12.modPatching.common.internal.FreezableContainer
import com.anatawa12.modPatching.internal.asFile
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.provideDelegate
import java.util.zip.ZipFile

class BinaryPatchImpl(
    override val mod: AbstractDownloadingMod,
) : BinaryPatch, FreezableContainer by FreezableContainer.Impl("added") {
    override var sourceTreeName: String by Delegates.freezable(mod.name)
    override fun getName(): String = mod.name

    fun onAdd() {
        require(mod.isFrozen()) { "mod is not added to container" }
        val project = mod.project

        val obfJarFile by lazy { ZipFile(mod.obfJarPath.asFile(project)) }

        fun isModifiedElement(file: FileTreeElement): Boolean {
            // not exists: not modified element, is an original.
            val entry = obfJarFile.getEntry(file.path) ?: return false
            // entry exists. return true if changed
            return file.size != entry.size
                    || !isSameDataStream(file.open(), obfJarFile.getInputStream(entry))
        }

        val jar = project.tasks.getByName("jar", Jar::class)
        project.tasks.getByName(COPY_MODIFIED_CLASSES, Copy::class).apply {
            dependsOn(mod.downloadTaskName)
            inputs.file(mod.obfJarPath)
            from(project.provider { project.zipTree(jar.archiveFile) }) {
                include { it.path.endsWith(".class") && isModifiedElement(it) }
            }
        }
        project.tasks.getByName(CHECK_SIGNATURE, CheckSignatureModification::class).apply {
            baseClasses.add(project.provider { project.zipTree(mod.obfJarPath) })
        }
        project.tasks.getByName(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class).apply {
            dependsOn(mod.downloadTaskName)
            oldFiles.add(project.provider { project.zipTree(mod.obfJarPath) })
        }
    }
}
