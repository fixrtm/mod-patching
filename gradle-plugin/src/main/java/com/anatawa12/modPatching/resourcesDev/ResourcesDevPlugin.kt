package com.anatawa12.modPatching.resourcesDev

import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class ResourcesDevPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(ModPatchingCommonPlugin::class)
    }
}
