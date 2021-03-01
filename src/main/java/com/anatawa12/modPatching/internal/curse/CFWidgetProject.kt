package com.anatawa12.modPatching.internal.curse

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CFWidgetProject(
    val id: Int,
    val files: List<CFWidgetFile>,
)
