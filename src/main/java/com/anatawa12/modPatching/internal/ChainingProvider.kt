package com.anatawa12.modPatching.internal

import java.util.concurrent.Callable

class ChainingProvider<T>(initial: T): Callable<T> {
    private var provider: Callable<T> = Callable { initial }
    fun then(block: (T) -> T) {
        val provider = provider
        this.provider = Callable { block(provider.call()) }
    }

    override fun call(): T = provider.call()
}
