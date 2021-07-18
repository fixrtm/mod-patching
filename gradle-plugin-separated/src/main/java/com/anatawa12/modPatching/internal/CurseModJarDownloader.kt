package com.anatawa12.modPatching.internal

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.File
import java.io.IOException

object CurseModJarDownloader {
    fun download(
        projectId: String,
        targetVersions: List<String>,
        versionName: String,
        destniation: File,
    ) {
        val client = HttpClientBuilder.create().build()
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

        client.execute(HttpGet(downloadUrl)).use { response ->
            if (response.statusLine.statusCode !in 200 until 300)
                throw IOException("unexpected code: $response")
            destniation.parentFile.mkdirs()

            response.entity.content.buffered().use { source ->
                destniation.outputStream().buffered().use { fos ->
                    source.copyTo(fos)
                }
            }
        }
    }
}
