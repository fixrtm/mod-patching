package com.anatawa12.modPatching.internal.curse

import com.squareup.moshi.Moshi
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
            return fileAdapter.fromJson(res.body!!.source())!!
        }
    }

    private val moshi = Moshi.Builder()
        .add { type, _, moshi -> if (type == ForgeSvcFile::class) ForgeSvcFileJsonAdapter(moshi) else null }
        .build()

    private val fileAdapter = moshi.adapter(ForgeSvcFile::class.java)
}
