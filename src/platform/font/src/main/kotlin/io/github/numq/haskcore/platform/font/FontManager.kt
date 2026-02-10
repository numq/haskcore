package io.github.numq.haskcore.platform.font

import kotlinx.atomicfu.atomic
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.readBytes
import kotlin.use

object FontManager : AutoCloseable {
    private const val FONTS_PATH = "fonts"

    const val DEFAULT_MONO_FONT = "JetBrainsMono-Regular.ttf"

    const val DEFAULT_SIZE = 13f

    const val DEFAULT_LINE_SPACING = 1.2f

    private val _isClosed = atomic(false)

    private val isClosed get() = _isClosed.value

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    val globalFontCollection: FontCollection by lazy {
        val provider = TypefaceFontProvider()

        provider.registerTypeface(loadFont(DEFAULT_MONO_FONT))

        FontCollection().apply {
            setAssetFontManager(provider)

            setDefaultFontManager(FontMgr.default)
        }
    }

    fun loadFont(fileName: String): Typeface {
        check(!isClosed) { "Font manager is closed" }

        return typefaceCache.computeIfAbsent(fileName) {
            val fullPath = "$FONTS_PATH/$fileName"

            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream(fullPath)?.use(InputStream::readBytes)) {
                "Font resource not found: $fullPath"
            }

            Data.makeFromBytes(bytes).use { data ->
                checkNotNull(FontMgr.default.makeFromData(data)) { "Failed to parse font data from $fullPath" }
            }
        }
    }

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) return

        if (globalFontCollection.isClosed.not()) {
            globalFontCollection.close()
        }

        typefaceCache.values.forEach(Typeface::close)

        typefaceCache.clear()
    }
}