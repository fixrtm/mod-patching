package com.anatawa12.modPatching.common.internal.curse

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ForgeSvcFile(
    val downloadUrl: String
)
