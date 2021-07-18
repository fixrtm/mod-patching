package com.anatawa12.modPatching.common.internal.curse

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal object CFWidgetApi {
    fun getProject(client: OkHttpClient, id: String): CFWidgetProject {
        val req = Request.Builder()
            .url("https://api.cfwidget.com/minecraft/mc-mods/$id")
            .get()
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful)
                throw IOException("unexpected code: $res")
            return Json.decodeFromString(CFWidgetProject.serializer(),
                res.body!!.source().readString(Charsets.UTF_8))
        }
    }
}
