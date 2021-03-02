package com.anatawa12.modPatching.internal

import org.gradle.util.GUtil

object Constants {
    val COPY_MODS_INTO_MODS_DIR = GUtil.toLowerCamelCase("copy mods into mods dir")
    val DOWNLOAD_MODS = GUtil.toLowerCamelCase("download mods")
    val DECOMPILE_MODS = GUtil.toLowerCamelCase("decompile mods")
    val GENERATE_UNMODIFIEDS = GUtil.toLowerCamelCase("generate unmodifieds")
    val PREPARE_MODS = GUtil.toLowerCamelCase("prepare mods")
    val COPY_MODIFIED_CLASSES  = GUtil.toLowerCamelCase("copy modified classes")
    val RENAME_SOURCE_NAME  = GUtil.toLowerCamelCase("rename source name")
    val GENERATE_BSDIFF_PATCH  = GUtil.toLowerCamelCase("generate bsdiff patch")
    val REPROCESS_RESOURCES  = GUtil.toLowerCamelCase("reprocess resources")
    val REGENERATE_JAR  = GUtil.toLowerCamelCase("regenerate jar")
}
