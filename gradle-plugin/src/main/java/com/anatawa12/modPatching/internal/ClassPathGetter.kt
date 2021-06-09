package com.anatawa12.modPatching.internal

import java.io.File

/**
 * the utility to get classpath of the class
 */
object ClassPathGetter {
    fun getOf(vararg classes: Class<*>): Set<File> {
        return classes.map { getOf(it) }.toSet()
    }
    fun getOf(clazz: Class<*>): File {
        return File(clazz.protectionDomain.codeSource.location.toURI())
    }
}
