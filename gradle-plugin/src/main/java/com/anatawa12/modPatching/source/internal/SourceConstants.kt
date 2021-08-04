package com.anatawa12.modPatching.source.internal

import org.gradle.util.GUtil

object SourceConstants {
    val DECOMPILE_MODS = GUtil.toLowerCamelCase("decompile mods")
    val INSTALL_SOURCE_UTIL_LOCALLY = "installSourceUtilLocally"
    val INSTALL_SOURCE_UTIL_GLOBALLY = "installSourceUtilGlobally"

    val MAPPING_CONFIGURATION = "modPatchingMapping"
    val FORGEFLOWER_CONFIGURATION = "modPatchingForgeFlower"
    val MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION = "modPatchingSourceUtilCli"
}
