package com.anatawa12.modPatching.binary.internal.signatureCheck

import com.anatawa12.modPatching.binary.internal.zipEitherByKey
import org.gradle.api.file.FileTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import java.io.File
import java.lang.reflect.Modifier

class SignatureChecker {
    private val differences = mutableListOf<Difference>()

    fun check(
        base: FileTree,
        modified: FileTree,
        rootPackage: String,
    ) {
        val rootPackagePath = rootPackage.replace('.', '/')
        differences.clear()

        val modifiedFiles = mutableMapOf<String, File>()
        modified.visit { modifiedFiles[this.path] = this.file }

        base.visit {
            if (!path.endsWith(".class")) return@visit
            if (!path.startsWith(rootPackagePath)) return@visit

            val modifiedFile = modifiedFiles[path] ?: return@visit

            val baseClass = readClass(file.readBytes())
            val modifiedClass = readClass(modifiedFile.readBytes())

            checkClass(path.removeSuffix(".class"), baseClass, modifiedClass)
        }
    }

    fun printDifferences(logger: Logger) {
        for (difference in differences) {
            when (difference) {
                is Difference.FieldOnlyInBase ->
                    logger.error("${difference.owner}.${difference.name}:${difference.desc} is only in base")
                is Difference.MethodOnlyInBase ->
                    logger.error("${difference.owner}.${difference.name}:${difference.desc} is only in base")
            }
        }
    }

    private fun checkClass(className: String, baseClass: ClassNode, modifiedClass: ClassNode) {
        val baseFields = baseClass.fields.map { it.name to it.desc to it }.toMap()
        val modifiedFields = modifiedClass.fields.map { it.name to it.desc to it }.toMap()
        baseFields.zipEitherByKey(modifiedFields).forEach { (k, v) ->
            val (name, desc) = k
            val (baseField, modifiedField) = v
            if (baseField == null) return@forEach
            if (Modifier.isPrivate(baseField.access)) return@forEach
            if (modifiedField == null) return@forEach addDiff(Difference.FieldOnlyInBase(className, name, desc))
        }

        val baseMethods = baseClass.methods.map { it.name to it.desc to it }.toMap()
        val modifiedMethods = modifiedClass.methods.map { it.name to it.desc to it }.toMap()
        baseMethods.zipEitherByKey(modifiedMethods).forEach { (k, v) ->
            val (name, desc) = k
            val (baseMethod, modifiedMethod) = v
            if (baseMethod == null) return@forEach
            if (Modifier.isPrivate(baseMethod.access)) return@forEach
            if (modifiedMethod == null) return@forEach addDiff(Difference.MethodOnlyInBase(className, name, desc))
        }
    }

    private fun readClass(byteArray: ByteArray): ClassNode = ClassNode().apply {
        ClassReader(byteArray)
            .accept(this, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES)
    }

    private fun addDiff(diff: Difference) {
        differences.add(diff)
    }
}

private sealed class Difference {
    data class FieldOnlyInBase(val owner: String, val name: String, val desc: String) : Difference()
    data class MethodOnlyInBase(val owner: String, val name: String, val desc: String) : Difference()
}
