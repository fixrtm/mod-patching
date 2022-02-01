# patching mod creating gradle plugin

[![a12 maintenance: Slowly](https://api.anatawa12.com/short/a12-slowly-svg)](https://api.anatawa12.com/short/a12-slowly-doc)
[![Gradle plugin: com.anatawa12.mod-patching.resources-dev](https://img.shields.io/gradle-plugin-portal/v/com.anatawa12.mod-patching.resources-dev?colorB=007ec6&label=gradle%20plugin:%20resources-dev&logo=gradle)](https://plugins.gradle.org/plugin/com.anatawa12.mod-patching.resources-dev)
[![Gradle plugin: com.anatawa12.mod-patching.source](https://img.shields.io/gradle-plugin-portal/v/com.anatawa12.mod-patching.source?colorB=007ec6&label=gradle%20plugin:%20source&logo=gradle)](https://plugins.gradle.org/plugin/com.anatawa12.mod-patching.source)
[![Gradle plugin: com.anatawa12.mod-patching.common](https://img.shields.io/gradle-plugin-portal/v/com.anatawa12.mod-patching.common?colorB=007ec6&label=gradle%20plugin:%20common&logo=gradle)](https://plugins.gradle.org/plugin/com.anatawa12.mod-patching.common)
[![Gradle plugin: com.anatawa12.mod-patching.binary](https://img.shields.io/gradle-plugin-portal/v/com.anatawa12.mod-patching.binary?colorB=007ec6&label=gradle%20plugin:%20binary&logo=gradle)](https://plugins.gradle.org/plugin/com.anatawa12.mod-patching.binary)
[![Maven Central: cli-tool](https://img.shields.io/maven-central/v/com.anatawa12.mod-patching/cli-tool?logo=apachemaven&label=maven:%20cli-tool)](https://mvnrepository.com/artifact/com.anatawa12.mod-patching/cli-tool/latest)
[![Maven Central: resources-dev-lib](https://img.shields.io/maven-central/v/com.anatawa12.mod-patching/resources-dev-lib?logo=apachemaven&label=maven:%20resources-dev-lib)](https://mvnrepository.com/artifact/com.anatawa12.mod-patching/resources-dev-lib/latest)

## Common (mod downloader)

This plugin adds task to download mod from mod providers. Currently, supports [curseforge].

### Configuration

```kotlin
plugins {
    id("com.anatawa12.mod-patching.common") version "2.1.0"
}

// add mod to download. the returned instance will be 
// used by binary or source patcher
val mod = mods.curse(id = "id", version = "version") {
    // slag of mod. e.g. "cofh-core"
    id = "id"
    // the version name contain in mod jar name.
    version = "version"
    // the target minecraft versions.
    targetVersions("1.12.2")
}
```

### Tasks

#### `downloadMods` - lifecycle task

Running this task will download all mods to cache directory.

#### `prepareMods` - lifecycle task

Running this task will prepare all mods to ready to use by develop with source/binary patching. With source patching
plugin, running this task will de-obfuscate mods.

#### `preparePatchingEnvironment` - lifecycle task

Running this task will make the workspace ready to patch. With source patching plugin, running this task will decompile
mods.

## Binary (bsdiff generator)

This plugin will add task to generate and embed bsdiff binary patches. To allow make binary patches after reobf task by
ForgeGradle, Generating binary patches is This depends on Common so `"com.anatawa12.mod-patching.common"` will
automatically be applied.

```kotlin
plugins {
    id("com.anatawa12.mod-patching.binary") version "2.1.0"
}

binPatching {
    // the mod instance by common
    patch(mod)
    // the base directory in jar to bsdiff.
    // with "your/package/bsdiff", the bsdiff patch for
    // "some/mod/SomeClass.class" will be 
    // at "your/package/bsdiff/some/mod/SomeClass.class.bsdiff"
    bsdiffPrefix = "your/package/bsdiff"
    // patching-mod appends this string to source name in class file.
    // "SomeClass.java" will be "SomeClass.java(modified by mod-name)"
    sourceNameSuffix = "(modified by mod-name)"
}

```

## Source (decompile+patch based mod developing)

This plugin will add tasks to develop a mod with decompiled source and applying&generating source patches.
See [documentation][source-patching-development] for more details.

```kotlin
plugins {
    id("com.anatawa12.mod-patching.source") version "2.1.0"
}

sourcePatching {
    // the name of mcp mapping.
    mappingName = "stable_39"
    // the mapping mc version.
    mcVersion = "1.12"
    // the version of forgeFlower, decompiler
    forgeFlowerVersion = "1.5.498.12"
    // true if you want to install pm.* utilities automatically
    autoInstallCli = true
    // the mod instance by common
    patch(mod) {
        // the name of directory to source files patches applied and patches
        // src/main/<sourceTreeName> for patched files and
        // src/main/<sourceTreeName>-patches for patch files
        sourceTreeName = mod.name
    }
}
```

default source format

```
src/main
 +- <sourceTreeName>
 |    // java source of <id>
 |    // if onRepo is PATCHES, gitignored.
 `- <sourceTreeName>-patches
      // patch source of <id>
      // is onRepo is not PATCHES, never created
```

## Resources For Development Running

This plugin will add tasks to use resources of other mod on development environment.

```kotlin
plugins {
    id("com.anatawa12.mod-patching.resources-dev") version "2.1.0"
}

// add one of them to fml.coreMods.load system property
// of running tasks.
println(resourcesDev.forgeFmlCoreModClassName)
println(resourcesDev.cpwFmlCoreModClassName)

resourcesDev {
    ofMod(rtm)
    ofMod(ngtlib)
}

```

[curseforge]: https://www.curseforge.com/minecraft/modpacks

[source-patching-development]: ./docs/source-patching-development.md
