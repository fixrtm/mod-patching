package com.anatawa12.modPatching.binary

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File

open class RenameSourceName : DefaultTask() {
    @OutputDirectory
    val classesDir = project.objects.property(File::class)
    @Input
    val suffix = project.objects.property(String::class)

    @TaskAction
    fun run() {
        for (file in classesDir.get().walkTopDown().filter { it.isFile }) {
            val cr = ClassReader(file.readBytes())
            val cw = ClassWriter(cr, 0)
            cr.accept(Visitor(cw), 0)
            file.writeBytes(cw.toByteArray())
        }
    }

    private inner class Visitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM6, cv) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source?.plus(suffix.get()), debug)
        }
    }
}
