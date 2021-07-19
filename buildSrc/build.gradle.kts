plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

val gradleRust by gradlePlugin.plugins.creating {
    id = "com.anatawa12.gradle-rust"
    implementationClass = "com.anatawa12.gradleRust.GradlePlugin"
}
