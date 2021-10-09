package com.anatawa12.modPatching.resourcesDev

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class ResourcesHelperGenerator : DefaultTask() {
    @Internal
    val jarFileList = project.objects.fileCollection()

    @Input
    protected val jarPathList = project.provider { jarFileList.map(File::getPath) }

    @OutputFile
    val output = project.objects.fileProperty()

    @Internal
    val outputFiles = project.provider { project.files(output.get()).builtBy(this) }

    @TaskAction
    fun execute() {
        val body = generateClassFile()
        val output = output.get().asFile
        output.parentFile.mkdirs()

        ZipOutputStream(output.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("com/anatawa12/modPatching/resourcesDev/lib/GeneratedHelper.class"))
            zos.write(body)
            zos.closeEntry()
        }
    }

    private fun generateClassFile(): ByteArray {
        val cw = ClassWriter(0)
        cw.visit(Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC,
            "com/anatawa12/modPatching/resourcesDev/lib/GeneratedHelper",
            null,
            "java/lang/Object",
            null)
        cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "init", "()V", null, null).apply {
            visitCode()
            for (jarFilePath in jarPathList.get()) {
                visitTypeInsn(Opcodes.NEW, "java/io/File")
                visitInsn(Opcodes.DUP)
                visitLdcInsn(jarFilePath)
                visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File",
                    "<init>", "(Ljava/lang/String;)V", false)
                visitMethodInsn(Opcodes.INVOKESTATIC, "com/anatawa12/modPatching/resourcesDev/lib/ResourcePackManager",
                    "addFile", "(Ljava/io/File;)V", false)
            }
            visitInsn(Opcodes.RETURN)
            visitEnd()
        }
        return cw.toByteArray()
    }
}
