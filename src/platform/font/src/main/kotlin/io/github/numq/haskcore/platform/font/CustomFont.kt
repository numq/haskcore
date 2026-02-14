package io.github.numq.haskcore.platform.font

import kotlinx.atomicfu.atomic
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TypefaceFontProvider

open class CustomFont(
    private val typeface: Typeface, val size: Float, lineSpacing: Float
) : AutoCloseable {
    private val _isClosed = atomic(false)

    private val isClosed get() = _isClosed.value

    private val fontProvider = TypefaceFontProvider().apply {
        registerTypeface(typeface)
    }

    private val collection = FontCollection().apply {
        setAssetFontManager(fontProvider)

        setDefaultFontManager(FontMgr.default)
    }

    private val font = Font(typeface = typeface, size = size).apply {
        edging = FontEdging.SUBPIXEL_ANTI_ALIAS

        hinting = FontHinting.SLIGHT

        isLinearMetrics = true

        isSubpixel = true

        setBitmapsEmbedded(true)
    }

    private val metrics = font.metrics

    val ascent = metrics.ascent

    val descent = metrics.descent

    val charWidth = font.getWidths(font.getStringGlyphs(" ")).first()

    val textHeight = descent - ascent

    val lineHeight = textHeight * lineSpacing

    val familyName = typeface.familyName

    fun createTextLine(text: String): TextLine {
        check(!isClosed) { "Font is closed" }

        return TextLine.make(text, font)
    }

    fun buildParagraph(style: ParagraphStyle, builder: ParagraphBuilder.() -> Unit): ParagraphBuilder {
        check(!isClosed) { "Font is closed" }

        return ParagraphBuilder(style = style, fc = collection).apply(builder)
    }

    fun measureTextWidth(text: String, paint: Paint? = null): Float {
        check(!isClosed) { "Font is closed" }

        return font.measureTextWidth(text, paint)
    }

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) return

        collection.close()

        fontProvider.close()

        font.close()
    }
}