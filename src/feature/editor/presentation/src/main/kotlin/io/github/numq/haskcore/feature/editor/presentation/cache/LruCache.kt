package io.github.numq.haskcore.feature.editor.presentation.cache

import arrow.core.Either
import io.github.numq.haskcore.feature.editor.core.cache.Cache
import kotlinx.atomicfu.atomic
import org.jetbrains.skia.impl.Managed

internal abstract class LruCache<Key, Value : Managed> : Cache<Key, Value> {
    private val _isClosed = atomic(false)

    private val isClosed get() = _isClosed.value

    abstract val capacity: Int

    abstract val factory: Key.() -> Value

    private val cache = LinkedHashMap<Key, Value>(capacity, .75f, true)

    private fun <T> withCache(block: () -> T) = Either.catch {
        synchronized(cache) {
            check(!isClosed) { "Cache is closed" }

            block()
        }
    }

    override fun getOrCreate(key: Key): Either<Throwable, Value> = Either.catch {
        synchronized(cache) {
            check(!isClosed) { "Cache is closed" }

            cache[key]?.let { return@catch it }
        }

        val newInstance = factory(key)

        synchronized(cache) {
            if (isClosed) {
                newInstance.close()

                error("Cache is closed")
            }

            val existing = cache[key]

            if (existing != null) {
                newInstance.close()

                return@catch existing
            }

            if (cache.size >= capacity) {
                val eldest = cache.entries.iterator()

                if (eldest.hasNext()) {
                    val entry = eldest.next()

                    if (!entry.value.isClosed) {
                        entry.value.close()
                    }

                    eldest.remove()
                }
            }

            cache[key] = newInstance

            newInstance
        }
    }

    override fun remove(key: Key) = withCache {
        cache.remove(key)?.takeUnless(Managed::isClosed)?.close()
    }.map {}

    override fun clear() = withCache {
        cache.values.forEach { value ->
            if (!value.isClosed) {
                value.close()
            }
        }

        cache.clear()
    }

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) return

        clear()
    }
}