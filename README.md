# patching mod creating gradle plugin
[![a12 maintenance: Slowly](https://anatawa12.com/short.php?q=a12-slowly-svg)](https://anatawa12.com/short.php?q=a12-slowly-doc)

Features (checked means implemented)

- [x] downloading mods from curse forge
- [x] obfuscating mods
- [ ] decompiling mods
- [ ] making binary patch
- [ ] making source patch
    - [ ] for workaround of decompiler bugs
    - [ ] for commits
- [ ] making git hooks
    - [ ] for commit only modified files
    - [ ] for commit only patches

```groovy
buildscript {
  mavenCentral()
  maven {
    name = "forge"
    url = "https://files.minecraftforge.net/maven"
  }
  dependencies {
    classpath("com.anatawa12.forge:ForgeGradle:2.3-1.0.+") {
      changing = true
    }
    classpath("com.anatawa12.mod-patching:mod-patching-gradle-plugin:1.0-SNAPSHOT")
  }
}

apply plugin: "com.anatawa12.mod-patching"
```

```kotlin
mods {
   curse {
      id = "id"
      version = "version"
      deobf = true // by default
      addToMods = true // by default if not patching

      // null if not add to configure
      // "compileOnly" by default if not patching.
      // if for patching, defaults null
      configurationAddTo = "compileOnly"

      // the target minecraft versions
      targetVersions("1.12.2")
   }
}

val rtm = mods.curse(id = "id", version = "version")

patching {
   patch(rtm) {
      sourceTreeName = "" // by default, "" means same as id.
      onRepo = OnRepoPatchSource.MODIFIEDS
      onRepo = OnRepoPatchSource.ALL
      onVCS = OnVCSPatchSource.MODIFIEDS
      onVCS = OnVCSPatchSource.PATCHES
      onVCS = OnVCSPatchSource.ALL_FILES // default
   }
}
```

default source format
```
src/main
 +- <sourceTreeName>-pre-patches 
 |    // patch source of <id> for workaround of decompiler bugs
 +- <sourceTreeName>
 |    // java source of <id>
 |    // if onRepo is PATCHES, gitignored.
 `- <sourceTreeName>-patches
      // patch source of <id>
      // is onRepo is not PATCHES, never created
```
