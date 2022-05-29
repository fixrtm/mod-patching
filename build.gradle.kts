plugins {
    id("com.anatawa12.compile-time-constant") version "1.0.5" apply false
    id("com.gradle.plugin-publish") version "0.21.0" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.6.21" apply false
    kotlin("plugin.serialization") version "1.6.21" apply false
}

group = "com.anatawa12.mod-patching"
version = providers.gradleProperty("version").forUseAtConfigurationTime().get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}
