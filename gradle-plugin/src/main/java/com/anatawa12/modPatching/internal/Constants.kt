package com.anatawa12.modPatching.internal

import org.gradle.util.GUtil

object Constants {
    val DECOMPILE_MODS = GUtil.toLowerCamelCase("decompile mods")
    val GENERATE_UNMODIFIEDS = GUtil.toLowerCamelCase("generate unmodifieds")
    val COPY_MODIFIED_CLASSES  = GUtil.toLowerCamelCase("copy modified classes")
    val CHECK_SIGNATURE = GUtil.toLowerCamelCase("check signature")
    val RENAME_SOURCE_NAME  = GUtil.toLowerCamelCase("rename source name")
    val GENERATE_BSDIFF_PATCH  = GUtil.toLowerCamelCase("generate bsdiff patch")
    val REPROCESS_RESOURCES  = GUtil.toLowerCamelCase("reprocess resources")
    val REGENERATE_JAR  = GUtil.toLowerCamelCase("regenerate jar")
}
