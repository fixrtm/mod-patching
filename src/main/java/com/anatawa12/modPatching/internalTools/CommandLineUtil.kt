@file:JvmName("CreateDiff")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.internal.indexOfFirst
import java.io.File
import java.io.IOException
import java.util.*

fun <T> Sequence<T>.chooseOne(
    maxSize: Int,
    kind: String,
    sc: Scanner,
    valueInfo: (T) -> String,
    emptyMsg: () -> Any,
    manyMsg: () -> Any,
    @Suppress("UNUSED_PARAMETER") dummy: Int = 0,
): T {
    val list = take(maxSize + 1).toList()
    if (list.isEmpty()) error(emptyMsg())
    if (list.size > maxSize) error(manyMsg())
    if (list.size == 1) return list[0]
    println("many $kind found. choose from list below.")
    for ((i, value) in list.withIndex()) {
        println("$i: ${valueInfo(value)}")
    }
    print("index:")
    System.out.flush()
    val index = sc.nextInt()
    if (index !in list.indices)
        error("invalid index: $index")
    return list[index]
}

fun makeJavaFileNameMatcher(matcher: String): (String) -> Boolean {
    val elements = matcher.split('.', '/', '\\')
    check (elements.all { it.isNotEmpty() }) { "some class name element is empty" }
    return matcher@{ fileName ->
        if (!fileName.endsWith(".java")) return@matcher false
        val name = fileName.removeSuffix(".java")
        var matchIndex = -1
        val fileNameElements = name.split('/')
        for (element in elements) {
            matchIndex = fileNameElements.indexOfFirst(matchIndex + 1) { it.contains(elements[0]) }
            if (matchIndex == -1) return@matcher false
        }
        true
    }
}

data class Difference<T>(
    val added: Set<T>,
    val deleted: Set<T>,
    val same: Set<T>,
) {
}

/**
 * @return Pair(added to this, deleted from this)
 */
fun <E> Set<E>.diff(
    other: Sequence<E>,
): Difference<E> {
    val both = mutableSetOf<E>()
    val notExistsOnThis = mutableSetOf<E>()
    val thisCopy = toMutableSet()
    for (otherElement in other) {
        if (!thisCopy.remove(otherElement)) {
            notExistsOnThis += otherElement
        } else {
            both += otherElement
        }
    }
    return Difference(thisCopy, notExistsOnThis, both)
}

fun <E> Set<E>.checkSame(
    other: Sequence<E>,
    notExistsOnThisMsg: String,
    notExistsOnOtherMsg: String,
) {
    val (addToThis, deletedFromThis) = diff(other)
    if (addToThis.isEmpty() && deletedFromThis.isEmpty()) return
    var error = ""
    if (addToThis.isNotEmpty()) {
        error += "$notExistsOnOtherMsg: \n" +
                addToThis.joinToString("\n") +
                "\n"
    }
    if (deletedFromThis.isNotEmpty()) {
        error += "$notExistsOnThisMsg: \n" +
                deletedFromThis.joinToString("\n") +
                "\n"
    }
    error(error)
}
