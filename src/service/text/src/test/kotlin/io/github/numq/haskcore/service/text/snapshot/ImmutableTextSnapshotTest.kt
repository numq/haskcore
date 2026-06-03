package io.github.numq.haskcore.service.text.snapshot

import io.github.numq.haskcore.common.core.text.TextLineEnding
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextRevision
import io.github.numq.haskcore.service.text.rope.Rope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets

internal class ImmutableTextSnapshotTest {
    private lateinit var emptyRope: Rope
    private lateinit var simpleRope: Rope
    private lateinit var multilineRope: Rope
    private val charset = StandardCharsets.UTF_8

    @BeforeEach
    fun setUp() {
        emptyRope = Rope("", charset)
        simpleRope = Rope("Hello World", charset)
        multilineRope = Rope("Hello\nWorld\nTest", charset)
    }

    @Test
    fun `constructor should create snapshot with correct properties`() {
        val snapshot = ImmutableTextSnapshot(
            rope = simpleRope,
            revision = TextRevision(value = 42L),
            charset = charset,
            textLineEnding = TextLineEnding.LF
        )

        assertEquals(42L, snapshot.revision.value)
        assertEquals(charset, snapshot.charset)
        assertEquals(TextLineEnding.LF, snapshot.textLineEnding)
    }

    @Test
    fun `lines should return total line count`() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(1, snapshot1.lines)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(1, snapshot2.lines)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(3, snapshot3.lines)
    }

    @Test
    fun `maxLineLength should return maximum line length`() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(0, snapshot1.maxLineLength)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(11, snapshot2.maxLineLength)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(5, snapshot3.maxLineLength)
    }

    @Test
    fun `lastPosition should return correct position`() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(TextPosition(0, 0), snapshot1.lastPosition)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(TextPosition(0, 11), snapshot2.lastPosition)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals(TextPosition(2, 4), snapshot3.lastPosition)
    }

    @Test
    fun `text should return full text with correct line endings`() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals("", snapshot1.text)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals("Hello World", snapshot2.text)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)
        assertEquals("Hello\nWorld\nTest", snapshot3.text)
    }

    @Test
    fun `text should convert line endings to CRLF when requested`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CRLF
        )
        assertEquals("Hello\r\nWorld\r\nTest", snapshot.text)
    }

    @Test
    fun `text should convert line endings to CR when requested`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CR
        )
        assertEquals("Hello\rWorld\rTest", snapshot.text)
    }

    @Test
    fun `isValidPosition should return true for valid positions`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertTrue(snapshot.isValidPosition(TextPosition(0, 0)))
        assertTrue(snapshot.isValidPosition(TextPosition(0, 5)))
        assertTrue(snapshot.isValidPosition(TextPosition(1, 5)))
        assertTrue(snapshot.isValidPosition(TextPosition(2, 4)))
    }

    @Test
    fun `isValidPosition should return false for invalid positions`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertFalse(snapshot.isValidPosition(TextPosition(3, 0)))
        assertFalse(snapshot.isValidPosition(TextPosition(0, 6)))
        assertFalse(snapshot.isValidPosition(TextPosition(1, 6)))
        assertFalse(snapshot.isValidPosition(TextPosition(2, 5)))
    }

    @Test
    fun `getLineText should return correct line text`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun `getLineText should throw exception for invalid line index`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertThrows<IllegalArgumentException> { snapshot.getLineText(-1) }
        assertThrows<IllegalArgumentException> { snapshot.getLineText(3) }
    }

    @Test
    fun `getLineText should handle empty snapshot`() {
        val snapshot = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals("", snapshot.getLineText(0))
    }

    @Test
    fun `getLineText should restore line endings correctly`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CRLF
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun `getLineLength should return correct line length`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals(5, snapshot.getLineLength(0))
        assertEquals(5, snapshot.getLineLength(1))
        assertEquals(4, snapshot.getLineLength(2))
    }

    @Test
    fun `getLineLength should handle empty snapshot`() {
        val snapshot = ImmutableTextSnapshot(emptyRope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals(0, snapshot.getLineLength(0))
    }

    @Test
    fun `getTextInRange should return correct substring`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val range = TextRange(TextPosition(0, 0), TextPosition(0, 5))
        assertEquals("Hello", snapshot.getTextInRange(range))
    }

    @Test
    fun `getTextInRange should return empty string for empty range`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val range = TextRange(TextPosition(0, 0), TextPosition(0, 0))
        assertEquals("", snapshot.getTextInRange(range))
    }

    @Test
    fun `getTextInRange should handle multiline ranges`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        val range = TextRange(TextPosition(0, 2), TextPosition(2, 2))
        assertEquals("llo\nWorld\nTe", snapshot.getTextInRange(range))
    }

    @Test
    fun `getTextInRange should restore line endings`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CRLF
        )

        val range = TextRange(TextPosition(0, 0), TextPosition(2, 4))
        assertEquals("Hello\r\nWorld\r\nTest", snapshot.getTextInRange(range))
    }

    @Test
    fun `getTextInRange should throw exception for invalid range`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        assertThrows<IllegalArgumentException> {
            TextRange(TextPosition(0, 6), TextPosition(0, 5))
        }
    }

    @Test
    fun `getBytePosition should return correct byte position`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 0))
        assertNotNull(bytePos)

        assertEquals(0, bytePos)
    }

    @Test
    fun `getBytePosition should return null for invalid position`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 10))
        assertNull(bytePos)
    }

    @Test
    fun `getBytePosition should handle UTF-8 multi-byte characters`() {
        val utf8Rope = Rope("Hello 世界", StandardCharsets.UTF_8)
        val snapshot = ImmutableTextSnapshot(utf8Rope, TextRevision(0), StandardCharsets.UTF_8, TextLineEnding.LF)

        val bytePos1 = snapshot.getBytePosition(TextPosition(0, 6))
        assertNotNull(bytePos1)
        assertEquals(6, bytePos1)

        val bytePos2 = snapshot.getBytePosition(TextPosition(0, 7))
        assertNotNull(bytePos2)
        assertEquals(9, bytePos2)

        val bytePos3 = snapshot.getBytePosition(TextPosition(0, 8))
        assertNotNull(bytePos3)
        assertEquals(12, bytePos3)
    }

    @Test
    fun `getBytePosition should handle UTF-16 encoding`() {
        val utf16Rope = Rope("Hello World", StandardCharsets.UTF_16)
        val snapshot = ImmutableTextSnapshot(utf16Rope, TextRevision(0), StandardCharsets.UTF_16, TextLineEnding.LF)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 5))
        assertNotNull(bytePos)
        assertEquals(12, bytePos)
    }

    @Test
    fun `getTextPosition should convert byte position to text position`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val position = snapshot.getTextPosition(5)
        assertEquals(TextPosition(0, 5), position)
    }

    @Test
    fun `getTextPosition should return null for invalid byte position`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        assertNull(snapshot.getTextPosition(-1))
        assertNull(snapshot.getTextPosition(100))
    }

    @Test
    fun `getTextPosition should handle zero byte position`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val position = snapshot.getTextPosition(0)
        assertEquals(TextPosition(0, 0), position)
    }

    @Test
    fun `getTextPosition should handle end of text`() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)

        val maxBytes = snapshot.getBytePosition(TextPosition(0, 11))
        val position = snapshot.getTextPosition(maxBytes!!)
        assertEquals(TextPosition(0, 11), position)
    }

    @Test
    fun `getTextPosition should handle multiline content`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        val worldBytePos = snapshot.getBytePosition(TextPosition(1, 0))
        val position = snapshot.getTextPosition(worldBytePos!!)
        assertEquals(TextPosition(1, 0), position)
    }

    @Test
    fun `getTextPosition should handle UTF-8 multi-byte characters`() {
        val utf8Rope = Rope("Hello 世界", StandardCharsets.UTF_8)
        val snapshot = ImmutableTextSnapshot(utf8Rope, TextRevision(0), StandardCharsets.UTF_8, TextLineEnding.LF)

        val position = snapshot.getTextPosition(6)
        assertEquals(TextPosition(0, 6), position)
    }

    @Test
    fun `snapshot should be immutable`() {
        val snapshot1 = ImmutableTextSnapshot(simpleRope, TextRevision(0), charset, TextLineEnding.LF)
        val originalText = snapshot1.text

        val newRope = simpleRope.insert(5, " Big")
        val snapshot2 = ImmutableTextSnapshot(newRope, TextRevision(1), charset, TextLineEnding.LF)

        assertEquals("Hello World", originalText)
        assertEquals("Hello Big World", snapshot2.text)
        assertNotEquals(originalText, snapshot2.text)
    }

    @Test
    fun `snapshot with CRLF line endings should maintain correct positions`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CRLF
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))

        assertEquals(TextPosition(2, 4), snapshot.lastPosition)
    }

    @Test
    fun `snapshot with CR line endings should work correctly`() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), charset = charset, textLineEnding = TextLineEnding.CR
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun `should handle very large lines correctly`() {
        val longLine = "A".repeat(10000)
        val rope = Rope(longLine, charset)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals(1, snapshot.lines)
        assertEquals(10000, snapshot.maxLineLength)
        assertEquals(TextPosition(0, 10000), snapshot.lastPosition)
        assertEquals(longLine, snapshot.text)
    }

    @Test
    fun `should handle many lines correctly`() {
        val lines = (1..1000).joinToString("\n") { "Line $it" }
        val rope = Rope(lines, charset)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), charset, TextLineEnding.LF)

        assertEquals(1000, snapshot.lines)
        assertEquals(8, snapshot.getLineLength(499))
        assertEquals("Line 500", snapshot.getLineText(499))
    }

    @Test
    fun `getTextPosition should find correct position via binary search`() {
        val lines = (1..100).joinToString("\n") { "Line $it" }
        val rope = Rope(lines, charset)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), charset, TextLineEnding.LF)

        val middleCharPos = snapshot.text.length / 2
        val bytePos = snapshot.text.substring(0, middleCharPos).toByteArray(charset).size

        val position = snapshot.getTextPosition(bytePos)
        assertNotNull(position)

        val recoveredBytePos = snapshot.getBytePosition(position!!)
        assertTrue(recoveredBytePos == bytePos || recoveredBytePos == bytePos + 1 || recoveredBytePos == bytePos - 1)
    }

    @Test
    fun `getTextPosition should handle positions at line boundaries`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        val pos1 = snapshot.getBytePosition(TextPosition(0, 5))
        val recovered1 = snapshot.getTextPosition(pos1!!)
        assertEquals(TextPosition(0, 5), recovered1)

        val pos2 = snapshot.getBytePosition(TextPosition(1, 0))
        val recovered2 = snapshot.getTextPosition(pos2!!)
        assertEquals(TextPosition(1, 0), recovered2)
    }

    @Test
    fun `getBytePosition should handle position after all text`() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), charset, TextLineEnding.LF)

        val lastPos = snapshot.lastPosition
        val bytePos = snapshot.getBytePosition(lastPos)
        assertNotNull(bytePos)

        val lastBytePos = snapshot.text.toByteArray(charset).size
        assertEquals(lastBytePos, bytePos)
    }

    @Test
    fun `getTextPosition should handle byte position at BOM boundary for UTF-16`() {
        val utf16Rope = Rope("Hello", StandardCharsets.UTF_16)
        val snapshot = ImmutableTextSnapshot(utf16Rope, TextRevision(0), StandardCharsets.UTF_16, TextLineEnding.LF)

        val position = snapshot.getTextPosition(2)
        assertEquals(TextPosition(0, 0), position)
    }
}