package com.anatawa12.modPatching.common.internal

import kotlin.reflect.KProperty

internal object Delegates {
    fun <T> lazy() = NonFreezableDelegatedValue<T>(DefaultHolder(notInitialized))

    fun <T> lazyFreezable() = FreezableDelegatedValue<T>(DefaultHolder(notInitialized))

    fun <T> freezable(init: T) = FreezableDelegatedValue<T>(init.wrapNull())

    fun <T> withDefault(default: () -> T) = NonFreezableDelegatedValue<T>(DefaultHolder { default() })

    fun <T> withDefaultFreezable(default: () -> T) = FreezableDelegatedValue<T>(DefaultHolder { default() })

    private val notInitialized: (String) -> Nothing = { error("$it is not initialized") }
}

internal class DefaultHolder<T>(val func: (String) -> T)

@Suppress("UNCHECKED_CAST")
private fun <T> processWrapped(valueInternal: Any?, name: String): T =
    (valueInternal as? DefaultHolder<T>)?.func?.invoke(name) ?: (valueInternal as T).unwrapNull<T>()

// init: NULL or T or DefaultHolder<T>
internal class FreezableDelegatedValue<T> (init: Any?) {
    internal var valueInternal: Any? = init

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: FreezableContainer, property: KProperty<*>): T =
        processWrapped(valueInternal, property.name)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: FreezableContainer, property: KProperty<*>, value: T) {
        thisRef.checkFreeze(property.name)
        this.valueInternal = value.wrapNull()
    }
}

// init: NULL or T or DefaultHolder<T>
internal class NonFreezableDelegatedValue<T> (init: Any?) {
    internal var valueInternal: Any? = init

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        processWrapped(valueInternal, property.name)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.valueInternal = value.wrapNull()
    }
}

private val NULL = Any()
@Suppress("UNCHECKED_CAST")
private fun <T : Any> T?.wrapNull(): T = this ?: NULL as T
@Suppress("UNCHECKED_CAST")
private fun <T> T.unwrapNull(): T =  this.takeUnless { it === NULL } as T
