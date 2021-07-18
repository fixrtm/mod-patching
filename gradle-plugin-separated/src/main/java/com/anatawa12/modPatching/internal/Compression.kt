package com.anatawa12.modPatching.internal

import java.io.OutputStream

fun interface Compression {
    fun apply(t: OutputStream): OutputStream
}
