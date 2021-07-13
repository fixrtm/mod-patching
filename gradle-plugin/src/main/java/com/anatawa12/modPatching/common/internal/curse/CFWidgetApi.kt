package com.anatawa12.modPatching.common.internal.curse

import com.squareup.moshi.Moshi
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
            return projectAdapter.fromJson(res.body!!.source())!!
        }
    }

    private val moshi = Moshi.Builder()
        .add { type, _, moshi -> if (type == CFWidgetProject::class) CFWidgetProjectJsonAdapter(moshi) else null }
        .add { type, _, moshi -> if (type == CFWidgetFile::class) CFWidgetFileJsonAdapter(moshi) else null }
        .build()

    private val projectAdapter = moshi.adapter(CFWidgetProject::class.java)
}
