package com.anatawa12.modPatching.internal

import kotlin.reflect.KProperty

internal object Delegates {
    fun <T> lazy() = FreezableLazyValueFactory<T>(
        checkFreezeFactory = noFreezeFactory,
        defaultFactory = notInitializedFactory,
        init = null,
    )

    fun <T> lazyFreezable() = OnContainerLazyValueFactory<T>(
        defaultFactory = notInitializedFactory,
        init = null,
    )

    fun <T> freezable(init: T) = OnContainerLazyValueFactory(
        defaultFactory = notInitializedFactory,
        init = init.wrapNull(),
    )

    fun <T> withDefault(default: () -> T) = FreezableLazyValueFactory(
        checkFreezeFactory = noFreezeFactory,
        defaultFactory = { default },
        init = null,
    )

    fun <T> withDefaultFreezable(default: () -> T) = OnContainerLazyValueFactory(
        defaultFactory = { default },
        init = null,
    )

    private val noFreezeFactory: (String) -> (() -> Unit) = {{}}
    private val notInitializedFactory: (String) -> (() -> Nothing) = {{ error("$it is not initialized") }}
}

internal class DelegatedValue<T> (
    val checkFreeze: () -> Unit,
    val default: () -> T,
    init: T?,
) {
    private var valueInternal: T? = init
    var value: T
        get() = (valueInternal ?: default()).unwrapNull()
        set(value) {
            checkFreeze()
            valueInternal = value.wrapNull()
        }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

internal class FreezableLazyValueFactory<T>(
    val checkFreezeFactory: (String) -> (() -> Unit),
    val defaultFactory: (String) -> (() -> T),
    val init: T?,
) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) =
        DelegatedValue(checkFreezeFactory(property.name), defaultFactory(property.name), init)
}

internal class OnContainerLazyValueFactory<T>(
    val defaultFactory: (String) -> (() -> T),
    val init: T?,
) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(thisRef: FreezableContainer, property: KProperty<*>) =
        DelegatedValue({ thisRef.checkFreeze(property.name) }, defaultFactory(property.name), init)
}

private val NULL = Any()
@Suppress("UNCHECKED_CAST")
private fun <T> T.wrapNull(): T = this ?: NULL as T
@Suppress("UNCHECKED_CAST")
private fun <T> T.unwrapNull(): T =  this.takeUnless { it === NULL } as T
