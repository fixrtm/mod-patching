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
import com.anatawa12.modPatching.source.internal.SourceConstants.FORGEFLOWER_CONFIGURATION
import com.anatawa12.modPatching.source.internal.SourceConstants.MAPPING_CONFIGURATION
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

class SourcePatchImpl(
    override val mod: AbstractDownloadingMod,
    val extension: SourcePatchingExtension,
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
        return mod.getJarPath("${extension.mappingChannel}-${extension.mappingVersion}-$classifier")
    }

    @FrozenByFreeze(of = "mod")
    val deobfJarPath by lazy { getMcpJarPath("deobf") }

    @FrozenByFreeze(of = "mod")
    val deobfTaskName by lazy { mod.getTaskName("deobfuscate") }

    fun onAdd(patchingDir: PatchingDir) {
        val project = mod.project
        val mainSourceSet = project.sourceSets["main"]
        mainSourceSet.java.srcDir(srcDirPath.asFile(project))

        val deobfTask = project.tasks.create(deobfTaskName, DeobfuscateSrg::class) {
            mappings.from(project.configurations.getByName(MAPPING_CONFIGURATION))

            sourceJar.set(mod.obfJarPath.asFile(project))
            destination.set(deobfJarPath.asFile(project))

            dependsOn(mod.downloadTaskName)
        }

        project.tasks.getByName(mod.prepareTaskName).dependsOn(deobfTask)

        val decompileTask = project.tasks.create(decompileTaskName, JavaExec::class) {
            dependsOn(deobfTaskName, mod.downloadTaskName)
            classpath = project.configurations.getByName(FORGEFLOWER_CONFIGURATION)

            args(
                "-din=1",
                "-rbr=1",
                "-dgs=1",
                "-asc=1",
                "-rsy=1",
                "-iec=1",
                "-jvn=1",
                "-log=TRACE",
                deobfJarPath,
                sourcesJarPath,
            )
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
