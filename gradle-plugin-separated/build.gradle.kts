plugins {
    id("com.anatawa12.compile-time-constant")
    id("com.github.johnrengelman.shadow")
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    maven(url = "https://maven.minecraftforge.net/") {
        name = "forge"
    }
}

dependencies {
    // gradle embedded library
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    // possible package name duplicate so shades
    implementation("com.anatawa12.jbsdiff:jbsdiff:1.0")
    implementation("com.charleskorn.kaml:kaml:0.44.0") {
        exclude("org.snakeyaml")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // libraries which I'll use: on classpath
    shadow("org.apache.httpcomponents:httpclient:4.5.13")
    shadow("org.ow2.asm:asm:9.3")
    shadow("org.ow2.asm:asm-commons:9.3")
    shadow("org.ow2.asm:asm-tree:9.3")
    shadow("org.snakeyaml:snakeyaml-engine:2.3")
    //shadow("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.2")
    //shadow("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
}

tasks.jar {
}

tasks.shadowJar {
    val basePkg = "com.anatawa12.modPatching.internal"
    from("license-info.md")
    relocate("kotlin.", "$basePkg.kotlin.")
    relocate("kotlinx.", "$basePkg.kotlinx.")
    relocate("com.charleskorn.kaml.", "$basePkg.kaml.")
    relocate("org.intellij.lang.annotations.", "$basePkg.ij_ann.")
    relocate("org.jetbrains.annotations.", "$basePkg.jb_ann.")
    relocate("io.sigpipe.jbsdiff.", "$basePkg.jbsdiff.")
}

tasks.createCompileTimeConstant {
    constantsClass = "com.anatawa12.modPatching.internal.SepConstants"
    values(
        mapOf(
            "DO_NOT_EDIT_HEADER" to file("do-not-edit-header.txt").readText(),
        )
    )
}

tasks.build.get().dependsOn(tasks.shadowJar.get())
