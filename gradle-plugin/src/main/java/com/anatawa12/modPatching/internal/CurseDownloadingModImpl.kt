package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.CurseDownloadingMod
import com.anatawa12.modPatching.DownloadCurseModJar
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.create
import org.gradle.util.GUtil.toLowerCamelCase
import java.io.File

class CurseDownloadingModImpl(project: Project) : AbstractDownloadingMod(project), CurseDownloadingMod {
    override var id: String by Delegates.lazyFreezable()
    override var version: String by Delegates.lazyFreezable()

    override var targetVersions: List<String> = emptyList()

    override val nameDefault: String get() = id

    override val cacheBaseDir get() = Util.getCachePath(project, "curse", id, version)
    override val cacheBaseName get() = Util.escapePathElement("$id-$version")
    override val modGlobalIdentifier get() = "curse-$id-$version"
    override fun configureDownloadingTask(dest: File): Task {
        return project.tasks.create(downloadTaskName, DownloadCurseModJar2::class) {
            mods = this@CurseDownloadingModImpl
            projectId = id
            versionName = version

            destniation = dest
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
