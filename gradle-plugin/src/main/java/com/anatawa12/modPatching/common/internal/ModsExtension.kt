package com.anatawa12.modPatching.common.internal

import com.anatawa12.modPatching.common.CurseDownloadingMod
import com.anatawa12.modPatching.common.DownloadingMod
import com.anatawa12.modPatching.common.ModsContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Namer
import org.gradle.api.Project

open class ModsExtension(private val project: Project) :
    ModsContainer,
    NamedDomainObjectCollection<DownloadingMod> by project.container(DownloadingMod::class.java)
{
    override fun getNamer(): Namer<DownloadingMod> = Namer { mod -> mod.name }

    override fun curse(block: Action<CurseDownloadingMod>): CurseDownloadingMod {
        return CurseDownloadingModImpl(project)
            .also(block::execute)
            .also(CurseDownloadingModImpl::freeze)
            .also { add(it) }
    }

    override fun curse(id: String?, version: String?, block: Action<CurseDownloadingMod>?): CurseDownloadingMod {
        return CurseDownloadingModImpl(project)
            .also { if (id != null) it.id = id }
            .also { if (version != null) it.version = version }
            .also { block?.execute(it) }
            .also(CurseDownloadingModImpl::freeze)
            .also { add(it) }
    }
}
