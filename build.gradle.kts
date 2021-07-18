plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    kotlin("jvm") version "1.5.10" apply false
    kotlin("plugin.serialization") version "1.5.10" apply false
}

group = "com.anatawa12.mod-patching"
version = "1.0.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
