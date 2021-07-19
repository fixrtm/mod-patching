# patching mod creating gradle plugin

[![a12 maintenance: Slowly](https://anatawa12.com/short.php?q=a12-slowly-svg)](https://anatawa12.com/short.php?q=a12-slowly-doc)

**Now working on rewriting to remove relationship to ForgeGradle. Documentation may not correct for latest SNAPSHOT.
See [at 85780a0]
for documentation for latest SNAPSHOT**

[at 85780a0]: https://github.com/anatawa12/mod-patching/tree/85780a0a28c9a7473d394b40ceef69f93f1bd906

Features (checked means implemented)

- [x] downloading mods from curse forge
- [x] obfuscating mods
- [x] decompiling mods
- [x] making binary patch
- [x] making source patch
  - [ ] for workaround of decompiler bugs
  - [x] for commits
- [ ] making git hooks
    - [ ] for commit patches
- [ ] making merge controller

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
      id = "id" // slag of mod. e.g. "cofh-core"
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
      sourceTreeName = "" // by default, id.
      onRepo = OnRepoPatchSource.MODIFIED // default
//      onRepo = OnRepoPatchSource.ALL
//      onVCS = OnVCSPatchSource.MODIFIED
      onVCS = OnVCSPatchSource.PATCHES // default
//      onVCS = OnVCSPatchSource.ALL_FILES
   }
}
```

default source format
```
src/main
// not yet implemented
// +- <sourceTreeName>-pre-patches 
// |    // patch source of <id> for workaround of decompiler bugs
 +- <sourceTreeName>
 |    // java source of <id>
 |    // if onRepo is PATCHES, gitignored.
 `- <sourceTreeName>-patches
      // patch source of <id>
      // is onRepo is not PATCHES, never created
```
