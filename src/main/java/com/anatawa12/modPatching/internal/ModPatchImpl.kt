package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.ModPatch
import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants.MOD_DIR_EXTENSION
import com.anatawa12.modPatching.internal.CommonConstants.MOD_ON_REPO_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCHING_DIR_NAME
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_DIR_PATH
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_JAR_PATH
import net.minecraftforge.gradle.tasks.fernflower.ApplyFernFlowerTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

class ModPatchImpl(
    override val mod: AbstractDownloadingMod,
) : ModPatch, FreezableContainer by FreezableContainer.Impl("added") {
    override var sourceTreeName: String by Delegates.freezable(mod.name)
    override var onRepo: OnRepoPatchSource by Delegates.freezable(OnRepoPatchSource.MODIFIED)
    override var onVCS: OnVCSPatchSource by Delegates.freezable(OnVCSPatchSource.PATCHES)
    override fun getName(): String = mod.name

    val decompileTaskName by lazy { mod.getTaskName("decompile") }
    val sourcesJarPathProvider by lazy {
        mod.project.provider {
            if (mod.deobf) mod.getMcpJarPath("deobf-sources") else mod.getJarPath("raw-sources")
        }
    }
    val srcDirPath by lazy { mod.project.file("src/main/$sourceTreeName") }

    fun onAdd() {
        val project = mod.project
        val mainSourceSet = project.sourceSets["main"]
        mainSourceSet.java.srcDir(srcDirPath)

        val decompileTask = project.tasks.create(decompileTaskName, ApplyFernFlowerTask::class) {
            setInJar(mod.finalJarProvider)
            setOutJar(sourcesJarPathProvider)
        }

        project.afterEvaluate {
            val patchingDir = project.projectDir.resolve(PATCHING_DIR_NAME)
            val modDir = patchingDir.resolve(Util.escapePathElement(mod.name) + ".$MOD_DIR_EXTENSION")
            modDir.resolve(".gitignore").writeText("""
                $SOURCE_JAR_PATH
                $SOURCE_DIR_PATH
                
            """.trimIndent())
            modDir.resolve(MOD_ON_REPO_CONFIG_FILE_NAME).writeText(onRepo.name)
            modDir.resolve(SOURCE_JAR_PATH)
                .writeText(project.file(sourcesJarPathProvider).absolutePath.escapePathStringForFile())
            modDir.resolve(SOURCE_DIR_PATH)
                .writeText(project.file(srcDirPath).absolutePath.escapePathStringForFile())
        }
    }
}
