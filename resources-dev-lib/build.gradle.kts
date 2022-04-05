plugins {
    `java-library`
    `maven-publish`
}

val `fml-api` by sourceSets.creating


repositories {
    mavenCentral()
    maven(url = "https://maven.minecraftforge.net/") {
        name = "forge"
        metadataSources {
            artifact()
        }
    }
    maven("https://libraries.minecraft.net/") {
        name = "mojang"
    }
}

dependencies {
    // minecraft launch-wrapper-based coreMod
    compileOnly(`fml-api`.output)
    compileOnly("net.minecraft:launchwrapper:1.12") {
        exclude("org.ow2.asm")
        exclude("org.apache.logging.log4j")
        exclude("net.sf.jopt-simple")
        exclude("org.lwjgl.lwjgl")
    }

    // the asm
    compileOnly("org.ow2.asm:asm:9.3")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
    configurePom {
        name.set("patching mod resources runtime processor")
        description.set("patching mod resources processor at runtime. this should be used by gradle plugin")
    }
}
