plugins {
    id("com.anatawa12.compile-time-constant")
    id("com.gradle.plugin-publish")
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
    signing
}

evaluationDependsOn(":gradle-plugin-separated")

repositories {
    mavenCentral()
    maven(url = "https://maven.minecraftforge.net/") {
        name = "forge"
    }
}

configurations.implementation {
    extendsFrom(project(":gradle-plugin-separated").configurations["shadow"])
}

dependencies {
    compileOnly(project(":gradle-plugin-separated")) {
        exclude("org.jetbrains.kotlin")
    }
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.tukaani:xz:1.9")

    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.snakeyaml:snakeyaml-engine:2.5")

    testImplementation(platform("io.kotest:kotest-bom:4.6.3"))
    testImplementation("io.kotest:kotest-framework-api")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testRuntimeOnly(platform("io.kotest:kotest-bom:4.6.3"))
    testRuntimeOnly("io.kotest:kotest-runner-junit5")
}

tasks.jar {
    val shadowJar by project(":gradle-plugin-separated").tasks.getting(Jar::class)
    dependsOn(shadowJar)
    from(provider { zipTree(shadowJar.archiveFile) })
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin.isAutomatedPublishing = false

pluginBundle {
    website = "https://github.com/anatawa12/mod-patching#readme"
    vcsUrl = "https://github.com/anatawa12/mod-patching.git"
    description = "the plugin for modifying some mod"
    mavenCoordinates.groupId = "${project.group}"
    tags = listOf("minecraft", "patch")
}

val pathingModCommon by gradlePlugin.plugins.creating {
    displayName = "Mod Patching Common Plugin"
    description = "The common plugin of Mod Patching"
    implementationClass = "com.anatawa12.modPatching.common.ModPatchingCommonPlugin"
    id = "com.anatawa12.mod-patching.common"
}

val pathingModBinary by gradlePlugin.plugins.creating {
    displayName = "Mod Patching Binary Plugin"
    description = "The binary patching plugin of Mod Patching"
    implementationClass = "com.anatawa12.modPatching.binary.BinaryPatchingPlugin"
    id = "com.anatawa12.mod-patching.binary"
}

val pathingModSource by gradlePlugin.plugins.creating {
    displayName = "Mod Patching Source Plugin"
    description = "The source patching plugin of Mod Patching"
    implementationClass = "com.anatawa12.modPatching.source.SourcePatchingPlugin"
    id = "com.anatawa12.mod-patching.source"
}

val resourcesOnDev by gradlePlugin.plugins.creating {
    displayName = "Mod Patching Resource Development Plugin"
    description = "The plugin for on-development environment resource configuration"
    implementationClass = "com.anatawa12.modPatching.resourcesDev.ResourcesDevPlugin"
    id = "com.anatawa12.mod-patching.resources-dev"
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
    configurePom {
        name.set("patching mod gradle plugin")
        description.set("A plugin to make a mod which patches some other mod.")
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication.name != "pathingModPluginMarkerMaven" }
}

tasks.createCompileTimeConstant {
    constantsClass = "com.anatawa12.modPatching.internal.Constants"
    values(
        mapOf(
            "VERSION_NAME" to project.version.toString(),
            "DO_NOT_EDIT_HEADER" to file("do-not-edit-header.txt").readText(),
        )
    )
}

java {
    withJavadocJar()
    withSourcesJar()
}
