package com.anatawa12.modPatching.common.internal

import org.gradle.api.Project
import java.io.File

object CommonUtil {
    fun getCacheBase(project: Project): File {
        return project.gradle.gradleUserHomeDir.resolve("caches/minecraft-mods")
    }
}
