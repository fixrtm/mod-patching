package com.anatawa12.modPatching.common

interface CurseDownloadingMod : DownloadingMod {
    var id: String
    var version: String
    var targetVersions: List<String>
    fun targetVersions(versions: List<String>) {
        targetVersions = targetVersions + versions
    }

    fun targetVersions(vararg versions: String) = targetVersions(versions.toList())
}
