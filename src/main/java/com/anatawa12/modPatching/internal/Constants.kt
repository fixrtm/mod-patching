package com.anatawa12.modPatching.internal

import org.gradle.util.GUtil

object Constants {
    val COPY_MODS_INTO_MODS_DIR = GUtil.toLowerCamelCase("copy mods into mods dir")
    val DOWNLOAD_MODS = GUtil.toLowerCamelCase("download mods")
    val DECOMPILE_MODS = GUtil.toLowerCamelCase("decompile mods")
    val GENERATE_UNMODIFIEDS = GUtil.toLowerCamelCase("generate unmodifieds")
    val PREPARE_MODS = GUtil.toLowerCamelCase("prepare mods")
}
