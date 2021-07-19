package com.anatawa12.modPatching.common

import com.anatawa12.modPatching.internal.CurseModJarDownloader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class DownloadCurseModJar : DefaultTask() {
    @Input
    lateinit var projectId: String

    @Input
    var targetVersions: List<String> = emptyList()
    @Input
    var versionName: String = ""

    @OutputFile
    lateinit var destniation: File

    @TaskAction
    open fun download() {
        CurseModJarDownloader.download(
            projectId,
            targetVersions,
            versionName,
            destniation,
        )
    }

}
