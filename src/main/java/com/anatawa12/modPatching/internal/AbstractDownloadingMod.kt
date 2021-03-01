package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.DownloadingMod
import com.anatawa12.modPatching.internal.Constants.COPY_MODS_INTO_MODS_DIR
import com.anatawa12.modPatching.internal.Constants.PREPARE_MODS
import net.minecraftforge.gradle.common.Constants.*
import net.minecraftforge.gradle.tasks.DeobfuscateJar
import net.minecraftforge.gradle.user.UserConstants
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.util.GUtil.toLowerCamelCase
import java.io.File

abstract class AbstractDownloadingMod(val project: Project) :
    DownloadingMod,
    FreezableContainer by FreezableContainer.Impl("added")
{
    override var deobf: Boolean = true

    internal val addToModsDefault: Boolean = true
    override var addToMods: Boolean by Delegates.withDefault(::addToModsDefault)

    protected abstract val nameDefault: String
    override var name: String by Delegates.withDefaultFreezable(::nameDefault)

    internal val configurationAddToDefault: String = "compileOnly"
    override var configurationAddTo: String? by Delegates.withDefault(::configurationAddToDefault)

    abstract val cacheBaseDir: File
    abstract val cacheBaseName: String
    abstract val modGlobalIdentifier: String
    abstract fun configureDownloadingTask(dest: File): Task

    fun getJarPath(classifier: String) = cacheBaseDir.resolve("$cacheBaseName-$classifier.jar")
    fun getMcpJarPath(classifier: String) = cacheBaseDir
        .resolve("$cacheBaseName-$REPLACE_MCP_CHANNEL-$REPLACE_MCP_VERSION-$classifier.jar")
        .let { project.provider(project.forgePlugin.delayedFile(it.toString())) }

    fun getTaskName(verb: String) = toLowerCamelCase("$verb mod $name")

    val obfJarPath by lazy { getJarPath("raw") }
    val deobfJarPathProvider by lazy { getMcpJarPath("deobf") }
    val finalJarProvider by lazy { project.provider { if (deobf) deobfJarPathProvider else obfJarPath } }

    val downloadTaskName by lazy { getTaskName("download") }
    val deobfTaskName by lazy { getTaskName("deobfuscate") }

    open fun onAdd() {
        val forgePlugin = project.forgePlugin

        val downloadTask = configureDownloadingTask(obfJarPath)
        val deobfTask = project.tasks.create(deobfTaskName, DeobfuscateJar::class) {
            setSrg(forgePlugin.delayedFile(SRG_NOTCH_TO_MCP))
            setExceptorJson(forgePlugin.delayedFile(MCP_DATA_EXC_JSON))
            setExceptorCfg(forgePlugin.delayedFile(EXC_MCP))
            setFieldCsv(forgePlugin.delayedFile(CSV_FIELD))
            setMethodCsv(forgePlugin.delayedFile(CSV_METHOD))

            setInJar(obfJarPath)
            setOutJar(deobfJarPathProvider)

            onlyIf { deobf }
            dependsOn(downloadTaskName,
                TASK_GENERATE_SRGS,
                UserConstants.TASK_EXTRACT_DEP_ATS,
                UserConstants.TASK_DD_COMPILE,
                UserConstants.TASK_DD_PROVIDED)
        }
        val copyIntoModsDirTask = project.tasks.create(toLowerCamelCase("copy into mods dir mod $name"), Copy::class) {
            from(finalJarProvider)
            into(project.provider { project.file(project.minecraft.runDir).resolve("mods") })

            onlyIf { addToMods }
            dependsOn(deobfTask)
        }

        val prepareTask = project.tasks.create(toLowerCamelCase("prepare mod $name"), DeobfuscateJar::class)
        prepareTask.dependsOn(downloadTask, deobfTask)

        project.tasks.getByName(COPY_MODS_INTO_MODS_DIR).dependsOn(copyIntoModsDirTask)
        project.tasks.getByName(PREPARE_MODS).dependsOn(prepareTask)
        project.afterEvaluate {
            configurationAddTo?.let { configurationAddTo ->
                dependencies {
                    configurationAddTo(files(finalJarProvider))
                }
            }
        }
    }
}
