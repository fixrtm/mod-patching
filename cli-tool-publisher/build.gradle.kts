plugins {
    `maven-publish`
}

val binaries = file("download").listFiles().orEmpty()
    .filter { it.name.startsWith("cli-") }
    .filter { it.resolve("mod-patching").exists() || it.resolve("mod-patching.exe").exists() }
    .map { file ->
        val binary = if (file.resolve("mod-patching").exists()) file.resolve("mod-patching")
        else file.resolve("mod-patching.exe")
        file.name.removePrefix("cli-") to binary
    }
println("configured for:")
for ((target, binary) in binaries) {
    println("$target: $binary")
}
val outDir = buildDir.resolve("artifacts")

val generateArtifacts by tasks.creating {
    doLast {
        outDir.mkdirs()
        for ((target, binary) in binaries) {
            binary.copyTo(outDir.resolve("mod-patching-$target.exe"),
                overwrite = true)
        }
    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifactId = "cli-tool"
    for ((target, _) in binaries) {
        artifact(outDir.resolve("mod-patching-$target.exe")) {
            builtBy(generateArtifacts)
            classifier = target
            extension = "exe"
        }
    }
    configurePom {
        name.set("patching mod console tools")
        description.set("The console utility to patch source code with pathcing-mod")
    }
}
