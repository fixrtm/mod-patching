plugins {
    `maven-publish`
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

/*
publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
    pom {
        name.set("patching mod gradle plugin")
        description.set("A plugin to make a mod which patches some other mod.")
        url.set("https://github.com/anatawa12/mod-patching#readme")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("anatawa12")
                name.set("anatawa12")
                email.set("anatawa12@icloud.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/anatawa12/mod-patching.git")
            developerConnection.set("scm:git:ssh://github.com:anatawa12/mod-patching.git")
            url.set("https://github.com/anatawa12/mod-patching")
        }
    }
}
 */
