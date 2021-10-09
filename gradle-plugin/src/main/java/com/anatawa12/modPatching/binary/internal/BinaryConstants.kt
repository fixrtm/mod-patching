package com.anatawa12.modPatching.binary.internal

object BinaryConstants {
    // list modified classes
    val LIST_MODIFIED_CLASSES = "listModifiedClasses"

    // Copies changed files to build/patching-mod/modified
    val COPY_MODIFIED_CLASSES = "copyModifiedClasses"

    // rename source file name for modified class files
    val RENAME_SOURCE_NAME = "renameSourceName"

    // generates bsdiff patch file
    val GENERATE_BSDIFF_PATCH = "generateBsdiffPatch"

    // regenerates patch files with
    val REGENERATE_JAR = "regenerateJar"

    // regenerates patch files with
    val COPY_JAR = "copyJar"

    // check binary compatibility between original and new one
    val CHECK_SIGNATURE = "checkSignature"
}
