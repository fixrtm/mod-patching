package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.common.internal.CommonUtil
import org.gradle.api.Project
import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, FUNCTION, VALUE_PARAMETER)
annotation class FrozenByFreeze(val of: String = "this")

fun RelativePathFromCacheRoot.asFile(project: Project) =
    CommonUtil.getCacheBase(project).resolve("$this")

fun RelativePathFromProjectRoot.asFile(project: Project) =
    project.projectDir.resolve("$this")
