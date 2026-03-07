package io.github.numq.haskcore.feature.editor.core.cache

import arrow.core.Either

interface Cache<Key, Value> : AutoCloseable {
    fun getOrCreate(key: Key): Either<Throwable, Value>

    fun remove(key: Key): Either<Throwable, Unit>

    fun clear(): Either<Throwable, Unit>
}