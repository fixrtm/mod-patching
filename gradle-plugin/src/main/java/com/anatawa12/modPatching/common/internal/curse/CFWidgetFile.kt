package com.anatawa12.modPatching.common.internal.curse

import kotlinx.serialization.Serializable

@Serializable
internal data class CFWidgetFile(
    val id: Long,
    val url: String,
    val display: String,
    val filesize: Long,
    // target versions
    val versions: Set<String>,
) {
    val displayName: String get() = display
    val targetVersions: Set<String> get() = versions
}
