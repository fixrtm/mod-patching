import com.anatawa12.gradleRust.CargoToolChain

plugins {
    id("com.anatawa12.gradle-rust")
    `maven-publish`
}

val cargoProj = cargo.projects.create("native") {
    projectDir.set(project.projectDir)
    targetName.set("mod-patching")
}

val target = findProperty("cli-tool-target")?.toString()
    ?: cargoProj.toolChain.get().getDefaultTarget()
val useCross = findProperty("cli-tool-cross").toString().toBoolean()
val cargoTarget = cargoProj.targets.create(target) {
    if (useCross) {
        println("configured to use cross")
        toolChain.set(CargoToolChain.cross)
    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(cargoTarget.binaryFile) {
        builtBy(cargoTarget.build)
        classifier = target
        extension = "exe"
    }
    configurePom {
        name.set("patching mod console tools")
        description.set("The console utility to patch source code with pathcing-mod")
    }
}
