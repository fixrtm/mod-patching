package com.anatawa12.modPatching.internal.curse

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ForgeSvcFile(
    val downloadUrl: String
)
