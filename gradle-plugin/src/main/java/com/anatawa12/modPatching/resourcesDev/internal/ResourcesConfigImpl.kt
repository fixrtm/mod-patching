package com.anatawa12.modPatching.resourcesDev.internal

import com.anatawa12.modPatching.common.internal.AbstractDownloadingMod
import com.anatawa12.modPatching.internal.asFile
import com.anatawa12.modPatching.resourcesDev.ResourcesConfig
import com.anatawa12.modPatching.resourcesDev.ResourcesHelperGenerator
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class ResourcesConfigImpl(override val mod: AbstractDownloadingMod) : ResourcesConfig {
    override fun getName(): String = mod.name

    val resourcesJarPath by lazy { mod.getJarPath("resources") }
    val generateResourcesTaskName by lazy { mod.getTaskName("generateResourcesJar") }

    fun onAdd(project: Project, helperTask: ResourcesHelperGenerator) {
        val resourcesTask = project.tasks.create(generateResourcesTaskName, Jar::class) {
            dependsOn(mod.downloadTaskName)
            from(project.zipTree(mod.obfJarPath.asFile(project)))
            // exclude class files
            exclude { it.path.endsWith(".class") }
            val path = resourcesJarPath.asFile(project)
            destinationDirectory.set(path.parentFile)
            archiveFileName.set(path.name)
        }
        project.dependencies.add("runtimeOnly",
            project.providers.provider { project.files(resourcesTask.archiveFile).builtBy(resourcesTask) })
        helperTask.dependsOn(resourcesTask)
        helperTask.jarFileList.from(resourcesTask)
    }
}
