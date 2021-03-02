@file:JvmName("CreateDiff")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.internal.indexOfFirst
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

fun <A, B> Set<A>.checkSame(
    other: Set<B>,
    mapper: (B) -> A,
    notExistsOnThisMsg: String,
    notExistsOnOtherMsg: String,
) {
    val notExistsOnThis = mutableSetOf<A>()
    val aCopy = toMutableSet()
    for (elementB in other) {
        val convertedB = mapper(elementB)
        if (!aCopy.remove(convertedB)) {
            notExistsOnThis += convertedB
        }
    }
    if (aCopy.isEmpty() && notExistsOnThis.isEmpty()) return
    var error = ""
    if (aCopy.isNotEmpty()) {
        error += "$notExistsOnOtherMsg: \n" +
                aCopy.joinToString("\n") +
                "\n"
    }
    if (notExistsOnThis.isNotEmpty()) {
        error += "$notExistsOnThisMsg: \n" +
                notExistsOnThis.joinToString("\n") +
                "\n"
    }
    error(error)
}
