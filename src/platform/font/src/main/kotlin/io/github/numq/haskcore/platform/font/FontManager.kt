package io.github.numq.haskcore.platform.font

import kotlinx.atomicfu.atomic
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.readBytes
import kotlin.use

internal class FontManager : AutoCloseable {
    private val _isClosed = atomic(false)

    private val isClosed get() = _isClosed.value

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    fun loadFont(fileName: String): Typeface {
        check(!isClosed) { "Font manager is closed" }

        return typefaceCache.computeIfAbsent(fileName) {
            val resourcePath = listOf(FontResources.FONTS_PATH, fileName).joinToString(separator = "/")

            val bytes = javaClass.classLoader.getResourceAsStream(resourcePath)?.use(InputStream::readBytes)

            Data.makeFromBytes(checkNotNull(bytes) {
                "Font resource not found: $resourcePath"
            }).use { data ->
                checkNotNull(FontMgr.default.makeFromData(data)) { "Failed to parse font data from $resourcePath" }
            }
        }
    }

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) return

        typefaceCache.values.forEach(Typeface::close)

        typefaceCache.clear()
    }
}