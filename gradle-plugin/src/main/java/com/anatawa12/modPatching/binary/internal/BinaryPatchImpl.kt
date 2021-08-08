package com.anatawa12.modPatching.binary.internal

import com.anatawa12.modPatching.binary.BinaryPatch
import com.anatawa12.modPatching.binary.CheckSignatureModification
import com.anatawa12.modPatching.binary.GenerateBsdiffPatch
import com.anatawa12.modPatching.binary.ListModifiedClasses
import com.anatawa12.modPatching.binary.internal.BinaryConstants.CHECK_SIGNATURE
import com.anatawa12.modPatching.binary.internal.BinaryConstants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.binary.internal.BinaryConstants.LIST_MODIFIED_CLASSES
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.common.internal.FreezableContainer
import com.anatawa12.modPatching.internal.asFile
import org.gradle.kotlin.dsl.getByName

class BinaryPatchImpl(
    override val mod: AbstractDownloadingMod,
) : BinaryPatch, FreezableContainer by FreezableContainer.Impl("added") {
    override fun getName(): String = mod.name

    fun onAdd() {
        require(mod.isFrozen()) { "mod is not added to container" }
        val project = mod.project

        project.tasks.getByName(LIST_MODIFIED_CLASSES, ListModifiedClasses::class).apply {
            dependsOn(mod.downloadTaskName)
            oldJars.from(project.files(mod.obfJarPath.asFile(project)))
        }
        project.tasks.getByName(CHECK_SIGNATURE, CheckSignatureModification::class).apply {
            baseClasses.add(project.provider { project.zipTree(mod.obfJarPath.asFile(project)) })
        }
        project.tasks.getByName(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class).apply {
            dependsOn(mod.downloadTaskName)
            oldFiles.add(project.provider { project.zipTree(mod.obfJarPath.asFile(project)) })
        }
    }
}
