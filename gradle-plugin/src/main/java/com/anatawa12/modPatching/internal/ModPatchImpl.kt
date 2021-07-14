package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.DeobfuscateSrg
import com.anatawa12.modPatching.GenerateBsdiffPatch
import com.anatawa12.modPatching.ModPatch
import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.binary.CheckSignatureModification
import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.common.internal.CommonUtil
import com.anatawa12.modPatching.common.internal.Delegates
import com.anatawa12.modPatching.common.internal.FreezableContainer
import com.anatawa12.modPatching.internal.Constants.CHECK_SIGNATURE
import com.anatawa12.modPatching.internal.Constants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.internal.Constants.DECOMPILE_MODS
import com.anatawa12.modPatching.internal.Constants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.internal.Constants.GENERATE_UNMODIFIEDS
import com.anatawa12.modPatching.internal.Constants.REPROCESS_RESOURCES
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import net.minecraftforge.gradle.common.Constants
import net.minecraftforge.gradle.tasks.fernflower.ApplyFernFlowerTask
import net.minecraftforge.gradle.user.UserConstants
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File
import java.util.zip.ZipFile

class ModPatchImpl(
    override val mod: AbstractDownloadingMod,
) : ModPatch, FreezableContainer by FreezableContainer.Impl("added") {
    override var sourceTreeName: String by Delegates.freezable(mod.name)
    override var onRepo: OnRepoPatchSource by Delegates.freezable(OnRepoPatchSource.MODIFIED)
    override var onVCS: OnVCSPatchSource by Delegates.freezable(OnVCSPatchSource.PATCHES)
    override fun getName(): String = mod.name

    val decompileTaskName by lazy { mod.getTaskName("decompile") }
    val generateUnmodifiedsJarTaskName by lazy { mod.getTaskName("generateUnmodifiedsJar") }

    val sourcesJarPathProvider by lazy {
        mod.project.provider {
            getMcpJarPathProvider("deobf-sources").get()
        }
    }
    val srcDirPath by lazy { mod.project.file("src/main/$sourceTreeName") }
    val patchDirPath by lazy { mod.project.file("src/main/$sourceTreeName-patches") }
    lateinit var unmodifiedsJarPath: File
        private set

    val buildDir: File get() = Util.getBuildPath(mod.project, "mods", name)
    fun getMcpJarPathProvider(classifier: String) = mod.cacheBaseDir
        .resolve("${mod.cacheBaseName}-${Constants.REPLACE_MCP_CHANNEL}-${Constants.REPLACE_MCP_VERSION}-$classifier.jar")
        .let { mod.project.provider(mod.project.forgePlugin.delayedFile(it.toString())) }

    val deobfJarPathProvider by lazy { getMcpJarPathProvider("deobf") }

    val obfJarPath by lazy { mod.getJarPath("raw") }
    val deobfTaskName by lazy { mod.getTaskName("deobfuscate") }

    fun onAdd(patchingDir: PatchingDir) {
        val project = mod.project
        val mainSourceSet = project.sourceSets["main"]
        mainSourceSet.java.srcDir(srcDirPath)

        val forgePlugin = project.forgePlugin
        val deobfTask = project.tasks.create(deobfTaskName, DeobfuscateSrg::class) {
            fieldCsv.set(project.provider(forgePlugin.delayedFile(Constants.CSV_FIELD)))
            methodCsv.set(project.provider(forgePlugin.delayedFile(Constants.CSV_METHOD)))

            sourceJar.set(obfJarPath)
            destination.set(deobfJarPathProvider)

            dependsOn(mod.downloadTaskName,
                Constants.TASK_GENERATE_SRGS,
                UserConstants.TASK_EXTRACT_DEP_ATS,
                UserConstants.TASK_DD_COMPILE,
                UserConstants.TASK_DD_PROVIDED)
        }

        project.tasks.getByName(mod.prepareTaskName).dependsOn(deobfTask)

        val modInfo = patchingDir.getOrNew(CommonUtil.escapePathElement(mod.name))

        val decompileTask = project.tasks.create(decompileTaskName, ApplyFernFlowerTask::class) {
            dependsOn(deobfTaskName, mod.downloadTaskName)
            classpath = project.files()
            forkedClasspath = project.configurations.getByName(Constants.CONFIG_FFI_DEPS)
            setInJar(deobfJarPathProvider)
            setOutJar(sourcesJarPathProvider)
        }
        project.tasks.getByName(DECOMPILE_MODS).dependsOn(decompileTask)

        val excludeClasses = project.provider { modInfo.modifiedClasses }
        fun isModifiedClass(fileName: String): Boolean {
            return excludeClasses.get().any {
                val classFile = it.replace('.', '/')
                classFile == fileName.removeSuffix(".class") || fileName.startsWith("$classFile$")
            }
        }
        if (onRepo == OnRepoPatchSource.MODIFIED) {
            val generateUnmodifiedsJarTask = project.tasks.create(generateUnmodifiedsJarTaskName, Zip::class) {
                dependsOn(deobfTaskName, mod.downloadTaskName)
                from(project.zipTree(deobfJarPathProvider))
                destinationDirectory.set(buildDir)
                archiveBaseName.set(mod.cacheBaseName)
                archiveClassifier.set("unmodifieds")
                archiveVersion.set("")
                archiveExtension.set("jar")
                unmodifiedsJarPath = archiveFile.get().asFile
                inputs.property("excludeClasses", excludeClasses)
                exclude { !it.path.endsWith(".class") || isModifiedClass(it.path) }
            }
            project.tasks.getByName(GENERATE_UNMODIFIEDS).dependsOn(generateUnmodifiedsJarTask)
            project.dependencies.add("implementation", project.files(unmodifiedsJarPath))
        }

        val jar = project.tasks.getByName("jar", Jar::class)
        project.tasks.getByName(COPY_MODIFIED_CLASSES, Copy::class).apply {
            dependsOn(jar)
            inputs.property("excludeClassesOf${mod.name}", excludeClasses)
            from(project.provider { project.zipTree(jar.archiveFile) }) {
                include { it.path.endsWith(".class") && isModifiedClass(it.path) }
            }
        }
        project.tasks.getByName(CHECK_SIGNATURE, CheckSignatureModification::class).apply {
            baseClasses.add(project.provider { project.zipTree(mod.obfJarPath) })
        }
        project.tasks.getByName(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class).apply {
            dependsOn(mod.downloadTaskName)
            oldFiles.add(project.provider {
                project.zipTree(mod.obfJarPath).matching {
                    include { it.path.endsWith(".class") && isModifiedClass(it.path) }
                }
            })
        }
        project.tasks.getByName(REPROCESS_RESOURCES, Copy::class).apply {
            dependsOn(mod.downloadTaskName)
            val inJarSpec: CopySpec by this.extra
            inJarSpec.exclude { elem -> ZipFile(mod.obfJarPath).use { it.getEntry(elem.path) != null } }
        }
        project.tasks.getByName("processResources", Copy::class).apply {
            dependsOn(mod.downloadTaskName)
            from(project.zipTree(mod.obfJarPath)) {
                exclude("**/*.class")
            }
        }

        project.afterEvaluate {
            modInfo.onRepo = onRepo
            modInfo.onVcs = onVCS
            modInfo.sourceJarPath = project.file(sourcesJarPathProvider)
            modInfo.sourcePath = project.file(srcDirPath)
            modInfo.patchPath = project.file(patchDirPath)

            onVCS
            when (onRepo) {
                OnRepoPatchSource.MODIFIED -> {
                    // reassign to write file
                    modInfo.modifiedClasses = modInfo.modifiedClasses
                }
            }
            modInfo.flush()
        }
    }
}
