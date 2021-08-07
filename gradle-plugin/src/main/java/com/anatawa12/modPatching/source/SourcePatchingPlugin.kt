package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.common.ModPatchingCommonPlugin
import com.anatawa12.modPatching.common.internal.CommonConstants.PREPARE_PATCHING_ENVIRONMENT
import com.anatawa12.modPatching.internal.Constants.VERSION_NAME
import com.anatawa12.modPatching.internal.PatchingDir
import com.anatawa12.modPatching.source.internal.*
import com.anatawa12.modPatching.source.internal.SourceConstants.DECOMPILE_MODS
import com.anatawa12.modPatching.source.internal.SourceConstants.FORGEFLOWER_CONFIGURATION
import com.anatawa12.modPatching.source.internal.SourceConstants.INSTALL_SOURCE_UTIL_GLOBALLY
import com.anatawa12.modPatching.source.internal.SourceConstants.INSTALL_SOURCE_UTIL_LOCALLY
import com.anatawa12.modPatching.source.internal.SourceConstants.MAPPING_CONFIGURATION
import com.anatawa12.modPatching.source.internal.SourceConstants.MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import java.io.File

@Suppress("unused")
open class SourcePatchingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ModPatchingCommonPlugin::class)

        val patchingDir = PatchingDir.on(project.projectDir)

        val patches = SourcePatchingExtension(project)
        project.extensions.add(SourcePatchContainer::class.java, "sourcePatching", patches)
        patches.all { (this as? SourcePatchImpl)?.onAdd(patchingDir) }

        project.configurations.maybeCreate(MAPPING_CONFIGURATION)
        project.configurations.maybeCreate(FORGEFLOWER_CONFIGURATION)
        project.configurations.maybeCreate(MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION)

        project.dependencies.add(
            MAPPING_CONFIGURATION,
            project.provider {
                "de.oceanlabs.mcp" +
                        ":mcp_${patches.mappingChannel}" +
                        ":${patches.mappingVersion}-${patches.mcVersion}" +
                        "@zip"
            },
        )

        project.dependencies.add(
            FORGEFLOWER_CONFIGURATION,
            project.provider {
                "net.minecraftforge:forgeflower:${patches.forgeFlowerVersion}"
            },
        )

        project.dependencies.add(
            MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION,
            project.provider {
                "com.anatawa12.mod-patching:cli-tool:${VERSION_NAME}:${getDownloadTriple(project)}@exe"
            },
        )

        val decompileMods = project.tasks.create(DECOMPILE_MODS)

        val installSourceUtilLocally =
            project.tasks.create(INSTALL_SOURCE_UTIL_LOCALLY, InstallSourcePatchingUtil::class) {
                destination.set(project.layout.projectDirectory)
                prefix.set("pm")
            }

        project.tasks.create(INSTALL_SOURCE_UTIL_GLOBALLY, InstallSourcePatchingUtil::class) {
            outputs.cacheIf { false }
            destination.set(
                project.layout.dir(
                    project.providers
                        .gradleProperty("sourceUtilInstallation")
                        .forUseAtConfigurationTime()
                        .map { File(it) })
                    .orElse(project.provider {
                        throw IllegalStateException("please specify installation directory via " +
                                "'-PsourceUtilInstallation=/path/to/dir'")
                    }))
            prefix.set(project.providers
                .gradleProperty("binaryPrefix")
                .forUseAtConfigurationTime()
                .orElse(""))
        }

        project.tasks.getByName(PREPARE_PATCHING_ENVIRONMENT).apply {
            dependsOn(decompileMods)
            dependsOn(installSourceUtilLocally)
        }

        project.afterEvaluate {
            val logger = project.logger
            if (patches.autoInstallCli) {
                installSourceUtilLocally.install()
            }
            val noIgnoreWarn = project.providers
                .gradleProperty("com.anatawa12.mod-patching.no-ignore-warn")
                .forUseAtConfigurationTime().orNull
                .equals("true", ignoreCase = true)
            if (!noIgnoreWarn) {
                val gitignore = projectDir.resolve(".gitignore").readTextOr("")
                val prefix = installSourceUtilLocally.prefix.get()

                val files: MutableSet<String>
                val errors: String
                if (prefix == "") {
                    files = mutableSetOf("add-modify*", "apply-patches*", "create-diff*")
                    errors = "add-modify*, apply-patches*, and create-diff* are"
                } else {
                    files = mutableSetOf("$prefix.*")
                    errors = "$prefix.* is"
                }

                for (line in gitignore.lineSequence()) {
                    files.removeIf { line == it || line == "/$it" }
                }
                if (files.isNotEmpty()) {
                    logger.warn("$errors not git ignored! generated patching mod utility " +
                            "should be ignored because they're environment dependent.")
                }
            }
        }
    }

    private fun getDownloadTriple(project: Project): String {
        return project.providers
            .gradleProperty("com.anatawa12.patching-mod.cli.triple")
            .forUseAtConfigurationTime()
            .orElse(project.provider(::computeCurrentTargetTriple))
            .get()
    }

    companion object {
        private val targetTriples = mapOf(
            (OperatingSystem.MACOS to Architecture.X64) to "x86_64-apple-darwin",
            (OperatingSystem.MACOS to Architecture.ARM64) to "aarch64-apple-darwin",
            (OperatingSystem.WINDOWS to Architecture.ARM64) to "aarch64-pc-windows-msvc",
            (OperatingSystem.WINDOWS to Architecture.X86) to "i686-pc-windows-msvc",
            (OperatingSystem.WINDOWS to Architecture.X64) to "x86_64-pc-windows-msvc",
            (OperatingSystem.WINDOWS to Architecture.ARM64) to "aarch64-unknown-linux-musl",
            (OperatingSystem.WINDOWS to Architecture.ARM32) to "armv7-unknown-linux-musleabihf",
            (OperatingSystem.WINDOWS to Architecture.X86) to "i686-unknown-linux-musl",
            (OperatingSystem.WINDOWS to Architecture.X64) to "x86_64-unknown-linux-musl",
        )

        private fun computeCurrentTargetTriple(): String {
            val operatingSystem = OperatingSystem.current ?: inferenceError()
            val architecture = Architecture.current ?: inferenceError()
            return targetTriples[operatingSystem to architecture]
                ?: error("$operatingSystem on $architecture is not (yet) supported!" +
                        "please make issue on https://github.com/anatawa12/mod-patching/issues")
        }

        private fun inferenceError(): Nothing {
            error("OS or Arch inference failed! Is your OS one of MacOS, Windows, or Linux? " +
                    "If so, please make issue on https://github.com/anatawa12/mod-patching/issues")
        }
    }
}
