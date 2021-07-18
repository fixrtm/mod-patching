plugins {
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
}

tasks.jar {
    val shadowJar by project(":gradle-plugin-separated").tasks.getting(Jar::class)
    dependsOn(shadowJar)
    from(provider { zipTree(shadowJar.archiveFile) })
}

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

fun isReleaseBuild() = !version.toString().contains("SNAPSHOT")

fun getReleaseRepositoryUrl(): String {
    return project.findProperty("RELEASE_REPOSITORY_URL")?.toString()
        ?: "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
}

fun getSnapshotRepositoryUrl(): String {
    return project.findProperty("SNAPSHOT_REPOSITORY_URL")?.toString()
        ?: "https://oss.sonatype.org/content/repositories/snapshots/"
}

signing {
    publishing.publications.forEach { publication ->
        sign(publication)
    }
}

publishing {
    repositories {
        maven {
            name = "mavenCentral"
            url = uri(if (isReleaseBuild()) getReleaseRepositoryUrl() else getSnapshotRepositoryUrl())

            credentials {
                username = project.findProperty("com.anatawa12.sonatype.username")?.toString() ?: ""
                password = project.findProperty("com.anatawa12.sonatype.passeord")?.toString() ?: ""
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication.name != "pathingModPluginMarkerMaven" }
}
