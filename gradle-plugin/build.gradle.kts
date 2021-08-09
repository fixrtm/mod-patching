plugins {
    id("com.anatawa12.compile-time-constant")
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
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.snakeyaml:snakeyaml-engine:2.3")
}

tasks.jar {
    val shadowJar by project(":gradle-plugin-separated").tasks.getting(Jar::class)
    dependsOn(shadowJar)
    from(provider { zipTree(shadowJar.archiveFile) })
}

gradlePlugin.isAutomatedPublishing = false

val pathingModCommon by gradlePlugin.plugins.creating {
    implementationClass = "com.anatawa12.modPatching.common.ModPatchingCommonPlugin"
    id = "com.anatawa12.mod-patching.common"
}

val pathingModBinary by gradlePlugin.plugins.creating {
    implementationClass = "com.anatawa12.modPatching.binary.BinaryPatchingPlugin"
    id = "com.anatawa12.mod-patching.binary"
}

val pathingModSource by gradlePlugin.plugins.creating {
    implementationClass = "com.anatawa12.modPatching.source.SourcePatchingPlugin"
    id = "com.anatawa12.mod-patching.source"
}

val resourcesOnDev by gradlePlugin.plugins.creating {
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
    values(mapOf("VERSION_NAME" to project.version.toString()))
}
