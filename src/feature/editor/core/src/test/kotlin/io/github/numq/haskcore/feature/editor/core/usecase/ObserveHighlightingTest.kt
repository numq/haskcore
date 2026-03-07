package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.right
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingToken
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingType
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.text.syntax.SyntaxScope
import io.github.numq.haskcore.service.text.syntax.SyntaxToken
import io.github.numq.haskcore.service.text.syntax.SyntaxTokenType
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ObserveHighlightingTest {
    private val editorService = mockk<EditorService>()
    private val textService = mockk<TextService>()

    private val highlightingRangeFlow = MutableStateFlow(IntRange.EMPTY)
    private val snapshotFlow = MutableStateFlow<TextSnapshot?>(null)
    private val editsFlow = MutableSharedFlow<TextEdit?>(extraBufferCapacity = 1)

    init {
        every { editorService.highlightingRange } returns highlightingRangeFlow
        every { textService.snapshot } returns snapshotFlow
        every { textService.edits } returns editsFlow
    }

    @Test
    fun `should fill gaps in line with UNKNOWN type`() = runTest {
        val useCase = ObserveHighlighting(editorService, textService)

        val lineText = "abcdefghij"
        val snapshot = mockk<TextSnapshot> {
            every { lines } returns 1
            every { maxLineLength } returns 10
            every { lastPosition } returns TextPosition(0, 10)
            every { getLineText(0) } returns lineText
            every { getLineLength(0) } returns 10
        }

        val syntaxToken = SyntaxToken(
            range = TextRange(TextPosition(0, 3), TextPosition(0, 7)), type = SyntaxTokenType.KEYWORD
        )

        snapshotFlow.value = snapshot
        highlightingRangeFlow.value = 0..0

        coEvery { textService.getScopes(any()) } returns emptyList<io.github.numq.haskcore.service.text.syntax.SyntaxScope>().right()
        coEvery { textService.getSyntaxTokens(any()) } returns listOf(syntaxToken).right()

        val highlighting = useCase(Unit).getOrElse { throw it }.first()

        val lineTokens = highlighting.tokensPerLine[0] ?: emptyList()

        assertEquals(3, lineTokens.size)

        assertEquals(HighlightingType.UNKNOWN, (lineTokens[0] as HighlightingToken.Region).type)
        assertEquals(0, lineTokens[0].range.start.column)
        assertEquals(3, lineTokens[0].range.end.column)

        assertEquals(HighlightingType.KEYWORD, (lineTokens[1] as HighlightingToken.Region).type)
        assertEquals(3, lineTokens[1].range.start.column)
        assertEquals(7, lineTokens[1].range.end.column)

        assertEquals(HighlightingType.UNKNOWN, (lineTokens[2] as HighlightingToken.Region).type)
        assertEquals(7, lineTokens[2].range.start.column)
        assertEquals(10, lineTokens[2].range.end.column)
    }

    @Test
    fun `should apply padding to requested range`() = runTest {
        val useCase = ObserveHighlighting(editorService, textService)

        val snapshot = mockk<TextSnapshot> {
            every { lines } returns 100
            every { maxLineLength } returns 10
            every { lastPosition } returns TextPosition(99, 10)
            every { getLineText(50) } returns "some text"
            every { getLineLength(any()) } returns 10
        }

        snapshotFlow.value = snapshot
        highlightingRangeFlow.value = 30..40

        coEvery { textService.getScopes(any()) } returns emptyList<SyntaxScope>().right()
        coEvery { textService.getSyntaxTokens(any()) } returns emptyList<SyntaxToken>().right()

        useCase(Unit).getOrElse { throw it }.first()

        val rangeSlot = slot<TextRange>()
        coVerify { textService.getSyntaxTokens(capture(rangeSlot)) }

        assertEquals(20, rangeSlot.captured.start.line)
        assertEquals(50, rangeSlot.captured.end.line)
    }
}