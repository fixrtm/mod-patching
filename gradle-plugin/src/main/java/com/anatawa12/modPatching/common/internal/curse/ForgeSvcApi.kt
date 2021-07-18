package com.anatawa12.modPatching.common.internal.curse

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ForgeSvcApi {
    fun getFileInfo(client: OkHttpClient, project: Long, file: Long): ForgeSvcFile {
        val req = Request.Builder()
            .url("https://addons-ecs.forgesvc.net/api/v2/addon/$project/file/$file")
            .get()
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful)
                throw IOException("unexpected code: $res")
            return Json.decodeFromString(ForgeSvcFile.serializer(),
                res.body!!.source().readString(Charsets.UTF_8))
        }
    }
}
