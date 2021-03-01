package com.anatawa12.modPatching

import org.gradle.api.Project

/**
 * this interface is not public for implementing
 */
interface DownloadingMod {
    /**
     * name of this mod.
     */
    var name: String

    /**
     * De-obfuscates downloaded mod if true.
     * Defaults true
     */
    var deobf: Boolean

    /**
     * Copy de-obfuscated downloaded jar into mods folder if true.
     * Defaults true if the mod is not for pathing. if it's for patching, false.
     */
    var addToMods: Boolean

    /**
     * The configuration add this mod jar to. If null, not added to any configurations.
     * Mod jar will be added on [Project.afterEvaluate] at this mod was added to [ModsContainer].
     * Defaults "compileOnly" if the mod is not for pathing. if it's for patching, `null`.
     */
    var configurationAddTo: String?
}
