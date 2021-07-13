package com.anatawa12.modPatching.common.internal

import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.CommonConstants.DOWNLOAD_MODS
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_MODS
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GUtil.toLowerCamelCase
import java.io.File

abstract class AbstractDownloadingMod(val project: Project) :
    DownloadingMod,
    FreezableContainer by FreezableContainer.Impl("added")
{
    protected abstract val nameDefault: String
    override var name: String by Delegates.withDefaultFreezable(::nameDefault)

    abstract val cacheBaseDir: File
    abstract val cacheBaseName: String
    abstract val modGlobalIdentifier: String
    abstract fun configureDownloadingTask(dest: File): Task

    fun getJarPath(classifier: String) = cacheBaseDir.resolve("$cacheBaseName-$classifier.jar")

    fun getTaskName(verb: String) = toLowerCamelCase("$verb mod $name")

    val obfJarPath by lazy { getJarPath("raw") }
    val downloadTaskName by lazy { getTaskName("download") }
    val prepareTaskName by lazy { getTaskName("prepare") }

    open fun onAdd() {
        val downloadTask = configureDownloadingTask(obfJarPath)

        val prepareTask = project.tasks.create(prepareTaskName)
        prepareTask.dependsOn(downloadTask)

        project.tasks.getByName(DOWNLOAD_MODS).dependsOn(downloadTask)
        project.tasks.getByName(PREPARE_MODS).dependsOn(prepareTask)
    }
}
