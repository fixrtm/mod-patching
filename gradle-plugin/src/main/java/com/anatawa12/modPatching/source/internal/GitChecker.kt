package com.anatawa12.modPatching.source.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.charset.Charset

object GitChecker {
    fun checkForRepo(project: Project, prefix: String) {
        val gitRepoDir = detectGitRepo(project.projectDir)
        if (gitRepoDir != null)
            checkIgnoreAndEol(project, prefix)
    }

    private fun detectGitRepo(root: File): File? {
        var dir = root
        while (true) {
            if (dir.resolve(".git").exists())
                return dir
            dir = dir.parentFile ?: return null
        }
    }

    private fun checkIgnoreAndEol(
        project: Project,
        prefix: String,
    ) {
        val logger = project.logger
        val gitCommand = project.providers
            .gradleProperty("com.anatawa12.mod-patching.git")
            .forUseAtConfigurationTime()
            .getOrElse("git")

        // check installed
        val installed = ProcessBuilder(gitCommand).command("--version").start().exitValue() == 0
        if (!installed) {
            logger.warn("found git repository but git binary not found!")
            logger.warn("Please set `com.anatawa12.mod-patching.git=path/to/git/binary`")
            return
        }

        checkGitIgnore(logger, gitCommand, project.projectDir, prefix)
    }

    private fun checkGitIgnore(logger: Logger, gitCommand: String, dir: File, prefix: String) {
        val binaryNames = if (prefix.isEmpty()) setOf("add-modify", "apply-patches", "create-diff")
        else setOf("$prefix.add-modify", "$prefix.apply-patches", "$prefix.create-diff")
        // add .exe postfix
        val execNames = binaryNames + binaryNames.map { "$it.exe" }

        val process = ProcessBuilder(gitCommand)
            .command("check-ignore", "-z", "--stdin")
            .directory(dir)
            .start()
        process.outputStream.use {
            it.write(execNames.joinToString("\u0000").toByteArray(Charset.defaultCharset()))
        }

        if (process.exitValue() != 0) {
            logger.error("$gitCommand check-ignore -z --stdin exit with non-zero value.")
            logger.error("git configuration validation failed")
            return
        }

        val ignoredFiles = process.inputStream.bufferedReader(Charset.defaultCharset())
            .use { it.readText() }.split('\u0000').toSet()
        val notIgnoredFiles = execNames - ignoredFiles

        if (notIgnoredFiles.isNotEmpty()) {
            logger.warn("those of locally installed source patching utility is not ignored:")
            logger.warn(notIgnoredFiles.joinToString(", "))
            logger.warn("it's environment dependent binary file so it should be git ignored.")
            if (prefix.isEmpty())
                logger.warn("you should add ${binaryNames.joinToString(",") { "$it*" }} to .gitignore")
            else
                logger.warn("you should add $prefix.* to .gitignore")
        }
    }
}
