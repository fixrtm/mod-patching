package com.anatawa12.modPatching.internal

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import java.lang.reflect.Modifier

class SignatureChecker {
    private val differences = mutableListOf<Difference>()

    fun check(
        base: Sequence<Pair<String, () -> ByteArray>>,
        modifiedGetter: (String) -> ByteArray?,
        rootPackage: String,
    ) {
        val rootPackagePath = rootPackage.replace('.', '/')
        differences.clear()

        base.forEach { (path, bytesGetter) ->
            if (!path.endsWith(".class")) return@forEach
            if (!path.startsWith(rootPackagePath)) return@forEach

            val modifiedFile = modifiedGetter(path) ?: return@forEach

            val baseClass = readClass(bytesGetter())
            val modifiedClass = readClass(modifiedFile)

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

private fun <K, V> Map<K, V>.zipEitherByKey(other: Map<K, V>): Map<K, Pair<V?, V?>> {
    val result = mutableMapOf<K, Pair<V?, V?>>()
    for ((k, v) in this) {
        result[k] = v to other[k]
    }
    for (k in (other.keys - keys)) {
        result[k] = null to other[k]
    }
    return result
}
