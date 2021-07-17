package com.anatawa12.modPatching.common.internal

import com.anatawa12.modPatching.common.CurseDownloadingMod
import com.anatawa12.modPatching.common.DownloadCurseModJar
import com.anatawa12.modPatching.internal.RelativePathFromCacheRoot
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.create

class CurseDownloadingModImpl(project: Project) : AbstractDownloadingMod(project), CurseDownloadingMod {
    override var id: String by Delegates.lazyFreezable()
    override var version: String by Delegates.lazyFreezable()

    override var targetVersions: List<String> = emptyList()

    override val nameDefault: String get() = id

    override val cacheBaseDir get() = RelativePathFromCacheRoot("curse/$id/$version")
    override val cacheBaseName get() = "$id-$version"
    override val modGlobalIdentifier get() = "curse-$id-$version"
    override fun configureDownloadingTask(dest: RelativePathFromCacheRoot): Task {
        return project.tasks.create(downloadTaskName, DownloadCurseModJar2::class) {
            mods = this@CurseDownloadingModImpl
            projectId = id
            versionName = version

            destniation = dest.asFile(project)
        }
    }

    open class DownloadCurseModJar2 : DownloadCurseModJar() {
        @get:Internal
        lateinit var mods: CurseDownloadingModImpl

        override fun download() {
            targetVersions = mods.targetVersions
            super.download()
        }
    }
}
