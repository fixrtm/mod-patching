package com.anatawa12.modPatching.source.internal

import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.common.internal.Delegates
import com.anatawa12.modPatching.common.internal.FreezableContainer
import com.anatawa12.modPatching.internal.FrozenByFreeze
import com.anatawa12.modPatching.internal.RelativePathFromCacheRoot
import com.anatawa12.modPatching.internal.RelativePathFromProjectRoot
import com.anatawa12.modPatching.source.DeobfuscateSrg
import com.anatawa12.modPatching.source.ModPatch
import com.anatawa12.modPatching.source.internal.SourceConstants.DECOMPILE_MODS
import net.minecraftforge.gradle.common.Constants
import net.minecraftforge.gradle.tasks.fernflower.ApplyFernFlowerTask
import net.minecraftforge.gradle.user.UserConstants
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

class SourcePatchImpl(
    override val mod: AbstractDownloadingMod,
) : ModPatch, FreezableContainer by FreezableContainer.Impl("added") {
    init {
        require(mod.isFrozen()) { "the mod is not ready to make source patch." }
    }

    override var sourceTreeName: String by Delegates.freezable(mod.name)
    override fun getName(): String = mod.name

    val decompileTaskName by lazy { mod.getTaskName("decompile") }

    @FrozenByFreeze(of = "mod")
    val sourcesJarPath by lazy { getMcpJarPath("deobf-sources") }
    val srcDirPath by lazy { RelativePathFromProjectRoot("src/main/$sourceTreeName") }
    val patchDirPath by lazy { RelativePathFromProjectRoot("src/main/$sourceTreeName-patches") }
    lateinit var unmodifiedsJarPath: File
        private set

    @FrozenByFreeze(of = "mod")
    fun getMcpJarPath(classifier: String): RelativePathFromCacheRoot {
        val replacer = mod.project.forgePlugin.replacer
        return mod.getJarPath("${replacer.get("MAPPING_CHANNEL")}-${replacer.get("MAPPING_VERSION")}-$classifier")
    }

    @FrozenByFreeze(of = "mod")
    val deobfJarPath by lazy { getMcpJarPath("deobf") }

    @FrozenByFreeze(of = "mod")
    val deobfTaskName by lazy { mod.getTaskName("deobfuscate") }

    fun onAdd(patchingDir: PatchingDir) {
        val project = mod.project
        val mainSourceSet = project.sourceSets["main"]
        mainSourceSet.java.srcDir(srcDirPath.asFile(project))

        val forgePlugin = project.forgePlugin
        val deobfTask = project.tasks.create(deobfTaskName, DeobfuscateSrg::class) {
            fieldCsv.set(project.provider(forgePlugin.delayedFile(Constants.CSV_FIELD)))
            methodCsv.set(project.provider(forgePlugin.delayedFile(Constants.CSV_METHOD)))

            sourceJar.set(mod.obfJarPath.asFile(project))
            destination.set(deobfJarPath.asFile(project))

            dependsOn(mod.downloadTaskName,
                Constants.TASK_GENERATE_SRGS,
                UserConstants.TASK_EXTRACT_DEP_ATS,
                UserConstants.TASK_DD_COMPILE,
                UserConstants.TASK_DD_PROVIDED)
        }

        project.tasks.getByName(mod.prepareTaskName).dependsOn(deobfTask)

        val decompileTask = project.tasks.create(decompileTaskName, ApplyFernFlowerTask::class) {
            dependsOn(deobfTaskName, mod.downloadTaskName)
            classpath = project.files()
            forkedClasspath = project.configurations.getByName(Constants.CONFIG_FFI_DEPS)
            setInJar(deobfJarPath)
            setOutJar(sourcesJarPath)
        }
        project.tasks.getByName(DECOMPILE_MODS).dependsOn(decompileTask)

        project.dependencies.add("implementation", project.files(unmodifiedsJarPath))
        val buildDir = RelativePathFromProjectRoot(project.buildDir.relativeTo(project.projectDir).path)
            .join("patching-mod/mods/$name")

        patchingDir.main.mods[mod.name] = ModInfo(
            patchPath = "$patchDirPath",
            sourcePath = "$srcDirPath",
            unmodifiedsJar = "${buildDir.join("${mod.cacheBaseName}-unmodifieds.jar")}",

            sourceJar = "$sourcesJarPath",
            deobfJar = "$deobfJarPath",
        )
        patchingDir.save()
    }
}
