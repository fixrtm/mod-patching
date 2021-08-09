package com.anatawa12.modPatching.resourcesDev

import com.anatawa12.modPatching.common.DownloadingMod
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.util.ConfigureUtil

interface ResourcesContainer : NamedDomainObjectContainer<ResourcesConfig> {
    // the adapter class names.
    // please choose from
    val forgeFmlCoreModClassName: String
    val cpwFmlCoreModClassName: String

    fun ofMod(mod: DownloadingMod, block: Action<ResourcesConfig>): ResourcesConfig
    fun ofMod(mod: DownloadingMod, block: Closure<*>): ResourcesConfig = ofMod(mod, ConfigureUtil.configureUsing(block))
    fun ofMod(mod: DownloadingMod): ResourcesConfig = ofMod(mod) {}
}
