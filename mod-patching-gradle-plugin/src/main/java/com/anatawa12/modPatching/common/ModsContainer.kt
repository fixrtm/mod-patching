package com.anatawa12.modPatching.common

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.util.ConfigureUtil

interface ModsContainer : NamedDomainObjectCollection<DownloadingMod> {
    fun curse(block: Action<CurseDownloadingMod>): CurseDownloadingMod
    fun curse(block: Closure<*>): CurseDownloadingMod = curse(ConfigureUtil.configureUsing(block))
    fun curse(
        id: String? = null,
        version: String? = null,
        block: Action<CurseDownloadingMod>? = null,
    ): CurseDownloadingMod
}
