package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.ModPatch
import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource

class ModPatchImpl(
    override val mod: AbstractDownloadingMod,
) : ModPatch, FreezableContainer by FreezableContainer.Impl("added") {
    override var sourceTreeName: String by Delegates.freezable("")
    override var onRepo: OnRepoPatchSource by Delegates.freezable(OnRepoPatchSource.MODIFIED)
    override var onVCS: OnVCSPatchSource by Delegates.freezable(OnVCSPatchSource.PATCHES)
    override fun getName(): String = mod.name

    fun onAdd() {
        // TODO
    }
}
