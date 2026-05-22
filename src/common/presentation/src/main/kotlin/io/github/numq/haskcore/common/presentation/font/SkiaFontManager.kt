package io.github.numq.haskcore.common.presentation.font

import arrow.core.Either
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.readBytes
import kotlin.use

internal class SkiaFontManager : FontManager {
    private val _isClosed = atomic(false)

    private val isClosed get() = _isClosed.value

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    override suspend fun loadFont(fileName: String, size: Float, lineSpacing: Float) = Either.catch {
        check(!isClosed) { "Font manager is closed" }

        withContext(Dispatchers.IO) {
            val typeface = typefaceCache.computeIfAbsent(fileName) {
                val resourcePath = listOf(FontResources.FONTS_PATH, fileName).joinToString(separator = "/")

                val bytes = javaClass.classLoader.getResourceAsStream(resourcePath)?.use(InputStream::readBytes)

                Data.makeFromBytes(checkNotNull(bytes) {
                    "Font resource not found: $resourcePath"
                }).use { data ->
                    checkNotNull(FontMgr.default.makeFromData(data)) { "Failed to parse font data from $resourcePath" }
                }
            }

            Font(typeface = typeface, size = size, lineSpacing = lineSpacing)
        }
    }

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) return

        typefaceCache.values.forEach(Typeface::close)

        typefaceCache.clear()
    }
}