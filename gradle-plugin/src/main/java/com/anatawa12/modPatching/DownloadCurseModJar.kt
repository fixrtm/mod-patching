package com.anatawa12.modPatching

import com.anatawa12.modPatching.internal.FirstAndRestSequence
import com.anatawa12.modPatching.internal.curse.CFWidgetApi
import com.anatawa12.modPatching.internal.curse.ForgeSvcApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException

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
        projectId // check init
        this.versionName // check init
        destniation // check init

        val client = OkHttpClient()
        val project = CFWidgetApi.getProject(client, projectId)

        val candidates = project.files
            .asSequence()
            .filter { it.targetVersions.containsAll(targetVersions) }
            .filter { versionName in it.display }

        val file = candidates.iterator().let { iterator ->
            if (!iterator.hasNext())
                error("$projectId version $versionName targets $targetVersions not found")
            val result = iterator.next()
            if (iterator.hasNext())
                error("multiple $projectId version $versionName targets $targetVersions found: "
                        + FirstAndRestSequence(result, iterator).joinToString { it.display })
            result
        }

        println("file name is: ${file.displayName}, id: ${file.id}")

        val downloadUrl = ForgeSvcApi.getFileInfo(client, project.id, file.id).downloadUrl

        val response = client
            .newCall(
                Request.Builder()
                    .url(downloadUrl)
                    .get()
                    .build())
            .execute()

        if (!response.isSuccessful)
            throw IOException("unexpected code: $response")

        destniation.parentFile.mkdirs()
        response.body!!.source().use { source ->
            destniation.sink().buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }

}
