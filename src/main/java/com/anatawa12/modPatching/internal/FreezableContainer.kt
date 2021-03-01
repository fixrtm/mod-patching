package com.anatawa12.modPatching.internal

interface FreezableContainer {
    fun freeze()
    fun addFreezer(freezer: () -> Unit)
    fun checkFreeze(name: String)

    class Impl(val freezeWhen: String) : FreezableContainer {
        private val freezers = mutableListOf<() -> Unit>()
        private var frozen = false

        override fun freeze() {
            if (frozen) return
            freezers.forEach { it() }
            frozen = true
        }

        override fun addFreezer(freezer: () -> Unit) {
            freezers += freezer
        }

        override fun checkFreeze(name: String) {
            if (frozen)
                error("$name cannot be assigned after $freezeWhen")
        }
    }
}
