package com.anatawa12.modPatching.internal

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File

object SourceRenamer {
    fun rename(dir: File, suffix: String) {
        for (file in dir.walkTopDown().filter { it.isFile }) {
            if (file.extension == "class") {
                val cr = ClassReader(file.readBytes())
                val cw = ClassWriter(cr, 0)
                cr.accept(Visitor(cw, suffix), 0)
                file.writeBytes(cw.toByteArray())
            }
        }
    }

    private class Visitor(cv: ClassVisitor, val suffix: String) : ClassVisitor(Opcodes.ASM6, cv) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source?.plus(suffix), debug)
        }
    }
}
