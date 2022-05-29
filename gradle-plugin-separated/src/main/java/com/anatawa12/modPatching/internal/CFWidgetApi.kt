package com.anatawa12.modPatching.internal

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import java.io.IOException

internal object CFWidgetApi {
    fun getProject(client: CloseableHttpClient, id: String): CFWidgetProject {
        val req = HttpGet("https://api.cfwidget.com/minecraft/mc-mods/$id")
        client.execute(req).use { res ->
            if (res.statusLine.statusCode !in 200 until 300)
                throw IOException("unexpected response code: \n\t\trequrest: $req\n\t\t$res")
            return Json.decodeFromString(CFWidgetProject.serializer(),
                res.entity.content.reader().buffered().use { it.readText() })
        }
    }
}
