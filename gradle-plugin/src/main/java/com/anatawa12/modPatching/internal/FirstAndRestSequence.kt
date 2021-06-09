package com.anatawa12.modPatching.internal

internal class FirstAndRestSequence<E : Any>(first: E, val rest: Iterator<E>): Sequence<E> {
    var first: E? = first

    override fun iterator(): Iterator<E> {
        val first = first ?: error("can't iterate two times")
        this.first = null
        return IteratorImpl(first, rest)
    }

    private class IteratorImpl<E : Any>(first: E, val rest: Iterator<E>) : Iterator<E> {
        var first: E? = first
        override fun hasNext(): Boolean = first != null || rest.hasNext()

        override fun next(): E {
            first?.let { first ->
                this.first = null
                return first
            }
            return rest.next()
        }
    }
}
