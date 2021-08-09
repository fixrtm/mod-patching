package com.anatawa12.modPatching.resourcesDev

import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import com.anatawa12.modPatching.internal.Constants
import com.anatawa12.modPatching.resourcesDev.internal.ResourcesConfigImpl
import com.anatawa12.modPatching.resourcesDev.internal.ResourcesDevConstants.GENERATE_RESOURCE_HELPER_CLASS
import com.anatawa12.modPatching.resourcesDev.internal.ResourcesDevExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class ResourcesDevPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(ModPatchingCommonPlugin::class)
        target.plugins.apply(JavaPlugin::class)

        val extension = ResourcesDevExtension(target)
        target.extensions.add(ResourcesContainer::class, "resourcesDev", extension)

        val generateTask = target.tasks.create(GENERATE_RESOURCE_HELPER_CLASS, ResourcesHelperGenerator::class) {
            output.set(this.temporaryDir.resolve("generated.jar"))
        }

        target.dependencies.add("runtimeOnly",
            "com.anatawa12.mod-patching:resources-dev-lib:${Constants.VERSION_NAME}")
        target.dependencies.add("runtimeOnly", generateTask.outputFiles)

        extension.all { (this as? ResourcesConfigImpl)?.onAdd(target, generateTask) }
    }
}
