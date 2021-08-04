package com.anatawa12.modPatching.common.internal

import org.gradle.util.GUtil

object CommonConstants {
    val DOWNLOAD_MODS = GUtil.toLowerCamelCase("download mods")
    val PREPARE_MODS = GUtil.toLowerCamelCase("prepare mods")
    val PREPARE_PATCHING_ENVIRONMENT = "preparePatchingEnvironment"
}
