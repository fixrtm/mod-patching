package com.anatawa12.modPatching.binary.internal

import org.gradle.util.GUtil

object BinaryConstants {
    // Copies changed files to build/patching-mod/modified
    val COPY_MODIFIED_CLASSES = GUtil.toLowerCamelCase("copy modified classes")

    // rename source file name for modified class files
    val RENAME_SOURCE_NAME = GUtil.toLowerCamelCase("rename source name")

    // generates bsdiff patch file
    val GENERATE_BSDIFF_PATCH = GUtil.toLowerCamelCase("generate bsdiff patch")

    // regenerates patch files with
    val REGENERATE_JAR = GUtil.toLowerCamelCase("regenerate jar")

    // regenerates patch files with
    val COPY_JAR = GUtil.toLowerCamelCase("copy jar")

    // check binary compatibility between original and new one
    val CHECK_SIGNATURE = GUtil.toLowerCamelCase("check signature")
}
