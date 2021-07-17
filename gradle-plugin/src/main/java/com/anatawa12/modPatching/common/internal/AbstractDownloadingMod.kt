package com.anatawa12.modPatching.common.internal

import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.internal.CommonConstants.DOWNLOAD_MODS
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_MODS
import com.anatawa12.modPatching.internal.FrozenByFreeze
import com.anatawa12.modPatching.internal.RelativePathFromCacheRoot
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GUtil.toLowerCamelCase

abstract class AbstractDownloadingMod(val project: Project) :
    DownloadingMod,
    FreezableContainer by FreezableContainer.Impl("added") {
    protected abstract val nameDefault: String

    @FrozenByFreeze
    override var name: String by Delegates.withDefaultFreezable(::nameDefault)

    @FrozenByFreeze
    abstract val cacheBaseDir: RelativePathFromCacheRoot

    @FrozenByFreeze
    abstract val cacheBaseName: String

    @FrozenByFreeze
    abstract val modGlobalIdentifier: String
    abstract fun configureDownloadingTask(dest: RelativePathFromCacheRoot): Task

    @FrozenByFreeze
    fun getJarPath(classifier: String) = cacheBaseDir.join("$cacheBaseName-$classifier.jar")

    @FrozenByFreeze
    fun getTaskName(verb: String) = toLowerCamelCase("$verb mod $name")

    @FrozenByFreeze
    val obfJarPath by lazy { getJarPath("raw") }

    @FrozenByFreeze
    val downloadTaskName by lazy { getTaskName("download") }

    @FrozenByFreeze
    val prepareTaskName by lazy { getTaskName("prepare") }

    open fun onAdd() {
        val downloadTask = configureDownloadingTask(obfJarPath)

        val prepareTask = project.tasks.create(prepareTaskName)
        prepareTask.dependsOn(downloadTask)

        project.tasks.getByName(DOWNLOAD_MODS).dependsOn(downloadTask)
        project.tasks.getByName(PREPARE_MODS).dependsOn(prepareTask)
    }
}
