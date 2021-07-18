package com.anatawa12.modPatching.common.internal.curse

import kotlinx.serialization.Serializable

@Serializable
internal data class CFWidgetProject(
    val id: Long,
    val files: List<CFWidgetFile>,
)
