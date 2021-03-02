package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.GenerateBsdiffPatch
import com.anatawa12.modPatching.ModPatch
import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants.MODIFIED_CLASSES_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.MOD_DIR_EXTENSION
import com.anatawa12.modPatching.internal.CommonConstants.MOD_ON_REPO_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.MOD_ON_VCS_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCHING_DIR_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_DIR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_DIR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_JAR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.Constants.COPY_MODIFIED_CLASSES
import com.anatawa12.modPatching.internal.Constants.DECOMPILE_MODS
import com.anatawa12.modPatching.internal.Constants.GENERATE_BSDIFF_PATCH
import com.anatawa12.modPatching.internal.Constants.GENERATE_UNMODIFIEDS
import com.anatawa12.modPatching.internal.Constants.REPROCESS_RESOURCES
import net.minecraftforge.gradle.common.Constants
import net.minecraftforge.gradle.tasks.fernflower.ApplyFernFlowerTask
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
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
            if (mod.deobf) mod.getMcpJarPathProvider("deobf-sources").get()
            else mod.getJarPath("raw-sources")
        }
    }
    val srcDirPath by lazy { mod.project.file("src/main/$sourceTreeName") }
    val patchDirPath by lazy { mod.project.file("src/main/$sourceTreeName-patches") }
    lateinit var unmodifiedsJarPath: File
        private set

    fun onAdd() {
        mod.configurationAddTo = null
        mod.addToMods = false
        val project = mod.project
        val mainSourceSet = project.sourceSets["main"]
        mainSourceSet.java.srcDir(srcDirPath)

        val patchingDir = project.projectDir.resolve(PATCHING_DIR_NAME)
        val modDir = patchingDir.resolve(Util.escapePathElement(mod.name) + ".$MOD_DIR_EXTENSION")

        val decompileTask = project.tasks.create(decompileTaskName, ApplyFernFlowerTask::class) {
            dependsOn(mod.deobfTaskName, mod.downloadTaskName)
            classpath = project.files()
            forkedClasspath = project.configurations.getByName(Constants.CONFIG_FFI_DEPS)
            setInJar(mod.finalJarProvider)
            setOutJar(sourcesJarPathProvider)
        }
        project.tasks.getByName(DECOMPILE_MODS).dependsOn(decompileTask)

        val excludeClasses = project.provider {
            val lines = modDir.resolve(MODIFIED_CLASSES_CONFIG_FILE_NAME).readText().lines()
            lines.map { it.unescapeStringForFile().replace('.', '/') }
        }
        fun isModifiedClass(fileName: String): Boolean {
            return excludeClasses.get().any { classFile ->
                classFile == fileName.removeSuffix(".class") || fileName.startsWith("$classFile$")
            }
        }
        if (onRepo == OnRepoPatchSource.MODIFIED) {
            val generateUnmodifiedsJarTask = project.tasks.create(generateUnmodifiedsJarTaskName, Zip::class) {
                dependsOn(mod.deobfTaskName, mod.downloadTaskName)
                from(project.zipTree(mod.finalJarProvider))
                destinationDirectory.set(mod.buildDir)
                archiveBaseName.set(mod.cacheBaseName)
                archiveClassifier.set("unmodifieds")
                archiveVersion.set("")
                archiveExtension.set("jar")
                unmodifiedsJarPath = archiveFile.get().asFile
                exclude { elem ->
                    val relative = elem.relativePath.pathString
                    !relative.endsWith(".class") || excludeClasses.get().any { classFile ->
                        classFile == relative.removeSuffix(".class") || relative.startsWith("$classFile$")
                    }
                }
            }
            project.tasks.getByName(GENERATE_UNMODIFIEDS).dependsOn(generateUnmodifiedsJarTask)
            project.dependencies.add("implementation", project.files(unmodifiedsJarPath))
        }

        val jar = project.tasks.getByName("jar", Jar::class)
        project.tasks.getByName(COPY_MODIFIED_CLASSES, Copy::class).apply {
            dependsOn(jar)
            from(project.provider { project.zipTree(jar.archiveFile) }) {
                include { it.path.endsWith(".class") && isModifiedClass(it.path) }
            }
        }
        project.tasks.getByName(GENERATE_BSDIFF_PATCH, GenerateBsdiffPatch::class).apply {
            dependsOn(mod.downloadTaskName)
            val oldFilesProvider: ChainingProvider<FileTree> by this.extra
            oldFilesProvider.then {
                it + project.zipTree(mod.obfJarPath).matching {
                    include { it.path.endsWith(".class") && isModifiedClass(it.path) }
                }
            }
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
            modDir.mkdirs()
            modDir.resolve(".gitignore").writeText("""
                .gitignore
                $MOD_ON_REPO_CONFIG_FILE_NAME
                $SOURCE_JAR_PATH_CONFIG_FILE_NAME
                $SOURCE_DIR_PATH_CONFIG_FILE_NAME
                $PATCH_DIR_PATH_CONFIG_FILE_NAME
                
            """.trimIndent())
            modDir.resolve(MOD_ON_REPO_CONFIG_FILE_NAME).writeText(onRepo.name)
            modDir.resolve(MOD_ON_VCS_CONFIG_FILE_NAME).writeText(onVCS.name)
            modDir.resolve(SOURCE_JAR_PATH_CONFIG_FILE_NAME)
                .writeText(project.file(sourcesJarPathProvider).absolutePath.escapePathStringForFile())
            modDir.resolve(SOURCE_DIR_PATH_CONFIG_FILE_NAME)
                .writeText(project.file(srcDirPath).absolutePath.escapePathStringForFile())
            modDir.resolve(PATCH_DIR_PATH_CONFIG_FILE_NAME)
                .writeText(project.file(patchDirPath).absolutePath.escapePathStringForFile())

            onVCS
            when (onRepo) {
                OnRepoPatchSource.MODIFIED -> {
                    if (!modDir.resolve(MODIFIED_CLASSES_CONFIG_FILE_NAME).exists())
                        modDir.resolve(MODIFIED_CLASSES_CONFIG_FILE_NAME).writeText("")
                }
            }
        }
    }
}
