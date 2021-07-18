package com.anatawa12.modPatching.internal

import kotlinx.serialization.json.Json
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import java.io.IOException

object ForgeSvcApi {
    fun getFileInfo(client: CloseableHttpClient, project: Long, file: Long): ForgeSvcFile {
        val req = HttpGet("https://addons-ecs.forgesvc.net/api/v2/addon/$project/file/$file")
        client.execute(req).use { res ->
            if (res.statusLine.statusCode !in 200 until 300)
                throw IOException("unexpected code: $res")
            return Json.decodeFromString(ForgeSvcFile.serializer(),
                res.entity.content.reader().buffered().use { it.readText() })
        }
    }
}
