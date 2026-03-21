package io.github.numq.haskcore.service.text.rope

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RopeTest {
    private fun createRope(initialText: String) = Rope(initialText = initialText)

    @Test
    fun testInitialEmptyBuffer() {
        val rope = createRope("")

        Assertions.assertEquals(0, rope.totalChars)
        Assertions.assertEquals(0, rope.totalBytes)
        Assertions.assertEquals(1, rope.totalLines)
        Assertions.assertEquals(0, rope.maxLineLength)
    }

    @Test
    fun testInitialContent() {
        val text = "Hello World"
        val rope = createRope(text)

        Assertions.assertEquals(text.length, rope.totalChars)
        Assertions.assertEquals(text.toByteArray().size, rope.totalBytes)
        Assertions.assertEquals(1, rope.totalLines)
        Assertions.assertEquals(text.length, rope.maxLineLength)
        Assertions.assertEquals(text, rope.getText(0, text.length))
    }

    @Test
    fun testInsertAtBeginning() {
        var rope = createRope("World")
        rope = rope.insert(0, "Hello ")

        Assertions.assertEquals("Hello World", rope.getText(0, 11))
        Assertions.assertEquals(11, rope.totalChars)
    }

    @Test
    fun testInsertAtEnd() {
        var rope = createRope("Hello")
        rope = rope.insert(5, " World")

        Assertions.assertEquals("Hello World", rope.getText(0, 11))
        Assertions.assertEquals(11, rope.totalChars)
    }

    @Test
    fun testInsertInMiddle() {
        var rope = createRope("Hello World")
        rope = rope.insert(6, "Beautiful ")

        Assertions.assertEquals("Hello Beautiful World", rope.getText(0, 21))
        Assertions.assertEquals(21, rope.totalChars)
    }

    @Test
    fun testDeleteFromBeginning() {
        var rope = createRope("Hello World")
        rope = rope.delete(0, 6)

        Assertions.assertEquals("World", rope.getText(0, 5))
        Assertions.assertEquals(5, rope.totalChars)
    }

    @Test
    fun testDeleteFromEnd() {
        var rope = createRope("Hello World")
        rope = rope.delete(5, 6)

        Assertions.assertEquals("Hello", rope.getText(0, 5))
        Assertions.assertEquals(5, rope.totalChars)
    }

    @Test
    fun testDeleteFromMiddle() {
        var rope = createRope("Hello Beautiful World")
        rope = rope.delete(6, 10)

        Assertions.assertEquals("Hello World", rope.getText(0, 11))
        Assertions.assertEquals(11, rope.totalChars)
    }

    @Test
    fun testGetPartialText() {
        val rope = createRope("Hello World")

        Assertions.assertEquals("Hello", rope.getText(0, 5))
        Assertions.assertEquals("World", rope.getText(6, 5))
        Assertions.assertEquals("lo Wo", rope.getText(3, 5))
    }

    @Test
    fun testUnicodeCharacters() {
        val text = "Привет мир 🌍"
        val rope = createRope(text)

        Assertions.assertEquals(text.length, rope.totalChars)
        Assertions.assertEquals(text.toByteArray().size, rope.totalBytes)
        Assertions.assertEquals(text, rope.getText(0, text.length))
    }

    @Test
    fun testMultipleLines() {
        val text = "Line1\nLine2\nLine3"
        val rope = createRope(text)

        Assertions.assertEquals(3, rope.totalLines)
        Assertions.assertEquals(5, rope.maxLineLength)
    }

    @Test
    fun testLineCountAfterInsert() {
        var rope = createRope("Line1\nLine3")
        rope = rope.insert(6, "Line2\n")

        Assertions.assertEquals(3, rope.totalLines)
    }

    @Test
    fun testLineCountAfterDelete() {
        var rope = createRope("Line1\nLine2\nLine3")
        rope = rope.delete(6, 6)

        Assertions.assertEquals(2, rope.totalLines)
    }

    @Test
    fun testMaxLineLength() {
        val rope = createRope("Short\nVeryLongLine\nMid")

        Assertions.assertEquals(12, rope.maxLineLength)
    }

    @Test
    fun testGetByteOffset() {
        val text = "Hello мир"
        val rope = createRope(text)

        Assertions.assertEquals(0, rope.getByteOffset(0))
        Assertions.assertEquals(1, rope.getByteOffset(1))
        Assertions.assertEquals(6, rope.getByteOffset(6))
    }

    @Test
    fun testGetOffsetOfLine() {
        val rope = createRope("First\nSecond\nThird")

        Assertions.assertEquals(0, rope.getOffsetOfLine(0))
        Assertions.assertEquals(6, rope.getOffsetOfLine(1))
        Assertions.assertEquals(13, rope.getOffsetOfLine(2))
    }

    @Test
    fun testGetOffsetOfLineOutOfBounds() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Text")
            rope.getOffsetOfLine(5)
        }
    }

    @Test
    fun testComplexOperations() {
        var rope = createRope("Start")
        rope = rope.insert(5, "\nMiddle")
        rope = rope.delete(0, 6)
        rope = rope.insert(0, "New")

        Assertions.assertEquals("NewMiddle", rope.getText(0, 9))
        Assertions.assertEquals(9, rope.totalChars)
        Assertions.assertEquals(1, rope.totalLines)
    }

    @Test
    fun testEmptyGetText() {
        val rope = createRope("")

        Assertions.assertEquals("", rope.getText(0, 0))
    }

    @Test
    fun testDeleteEmpty() {
        var rope = createRope("Test")
        rope = rope.delete(2, 0)

        Assertions.assertEquals("Test", rope.getText(0, 4))
    }

    @Test
    fun testInsertEmpty() {
        var rope = createRope("Test")
        rope = rope.insert(2, "")

        Assertions.assertEquals("Test", rope.getText(0, 4))
    }

    @Test
    fun testInsertOutOfBounds() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.insert(10, "X")
        }
    }

    @Test
    fun testDeleteOutOfBounds() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.delete(2, 10)
        }
    }

    @Test
    fun testGetTextOutOfBounds() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.getText(2, 10)
        }
    }

    @Test
    fun testGetByteOffsetOutOfBounds() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.getByteOffset(10)
        }
    }

    @Test
    fun testConsecutiveOperations() {
        var rope = createRope("")
        rope = rope.insert(0, "A")
        rope = rope.insert(1, "B")
        rope = rope.insert(2, "C")
        rope = rope.delete(1, 1)
        rope = rope.insert(1, "D")

        Assertions.assertEquals("ADC", rope.getText(0, 3))
        Assertions.assertEquals(3, rope.totalChars)
    }

    @Test
    fun testNewlineHandling() {
        var rope = createRope("Line1\nLine2")

        Assertions.assertEquals(2, rope.totalLines)
        Assertions.assertEquals(5, rope.maxLineLength)

        rope = rope.insert(11, "\nLine3")

        Assertions.assertEquals(3, rope.totalLines)
    }

    @Test
    fun testMixedNewlineFormats() {
        val rope = createRope("Line1\r\nLine2\nLine3")

        Assertions.assertEquals(3, rope.totalLines)
    }

    @Test
    fun testGetByteOffsetWithUnicode() {
        val text = "aбc"
        val rope = createRope(text)

        Assertions.assertEquals(0, rope.getByteOffset(0))
        Assertions.assertEquals(1, rope.getByteOffset(1))
        Assertions.assertEquals(3, rope.getByteOffset(2))
    }

    @Test
    fun testLargeInsert() {
        var rope = createRope("")
        val largeText = "A".repeat(10000)
        rope = rope.insert(0, largeText)

        Assertions.assertEquals(10000, rope.totalChars)
        Assertions.assertEquals(largeText, rope.getText(0, 10000))
    }

    @Test
    fun testStressOperations() {
        var rope = createRope("Initial")

        repeat(101) {
            rope = rope.insert(0, "X")
        }
        Assertions.assertEquals(108, rope.totalChars)

        repeat(50) {
            if (rope.totalChars > 0) {
                rope = rope.delete(0, 1)
            }
        }
        Assertions.assertTrue(rope.totalChars > 0)
    }

    @Test
    fun testGetTextBeyondEnd() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.getText(3, 10)
        }
    }

    @Test
    fun testDeletePartial() {
        var rope = createRope("Hello World")
        rope = rope.delete(3, 5)

        Assertions.assertEquals("Helrld", rope.getText(0, 6))
    }

    @Test
    fun testInsertMultipleTimes() {
        var rope = createRope("")
        rope = rope.insert(0, "A")
        rope = rope.insert(1, "B")
        rope = rope.insert(2, "C")

        Assertions.assertEquals("ABC", rope.getText(0, 3))
    }

    @Test
    fun testInsertAtExactEnd() {
        var rope = createRope("Hello")
        rope = rope.insert(5, " World")

        Assertions.assertEquals("Hello World", rope.getText(0, 11))
    }

    @Test
    fun testInsertBeyondEnd() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.insert(6, " World")
        }
    }

    @Test
    fun testGetTextZeroLength() {
        val rope = createRope("Hello")

        Assertions.assertEquals("", rope.getText(0, 0))
        Assertions.assertEquals("", rope.getText(5, 0))
    }

    @Test
    fun testGetTextNegativeLength() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.getText(0, -1)
        }
    }

    @Test
    fun testDeleteNegativeLength() {
        assertThrows<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.delete(0, -1)
        }
    }

    @Test
    fun testMergeAdjacentOriginalPieces() {
        var rope = createRope("Hello World")
        rope = rope.delete(5, 1)

        Assertions.assertEquals("HelloWorld", rope.getText(0, 10))
    }

    @Test
    fun testLargeNumberOfPieces() {
        var rope = createRope("")
        for (i in 0..1000) {
            rope = rope.insert(i, "A")
        }

        Assertions.assertEquals(1001, rope.totalChars)
        Assertions.assertEquals("A".repeat(1001), rope.getText(0, 1001))
    }

    @Test
    fun testGetTextWithMultiplePieces() {
        var rope = createRope("Hello World")
        rope = rope.insert(6, "Beautiful ")
        rope = rope.delete(16, 1)
        rope = rope.insert(11, "!!!")

        val result = rope.getText(0, rope.totalChars)
        Assertions.assertEquals("Hello Beaut!!!iful orld", result)
    }

    @Test
    fun testGetOffsetOfLineWithMultipleNewlinesInPiece() {
        val rope = createRope("Line1\nLine2\nLine3\nLine4")

        Assertions.assertEquals(0, rope.getOffsetOfLine(0))
        Assertions.assertEquals(6, rope.getOffsetOfLine(1))
        Assertions.assertEquals(12, rope.getOffsetOfLine(2))
        Assertions.assertEquals(18, rope.getOffsetOfLine(3))
    }

    @Test
    fun testGetOffsetOfLineWithEmptyLines() {
        val rope = createRope("Line1\n\nLine3")

        Assertions.assertEquals(0, rope.getOffsetOfLine(0))
        Assertions.assertEquals(6, rope.getOffsetOfLine(1))
        Assertions.assertEquals(7, rope.getOffsetOfLine(2))
    }

    @Test
    fun testGetOffsetOfLineWithMixedNewlines() {
        val rope = createRope("Line1\r\nLine2\nLine3")

        Assertions.assertEquals(0, rope.getOffsetOfLine(0))
        Assertions.assertEquals(7, rope.getOffsetOfLine(1))
        Assertions.assertEquals(13, rope.getOffsetOfLine(2))
    }

    @Test
    fun testEmojiAndSurrogates() {
        val emoji = "🚀 "
        var rope = Rope("Hello World")
        rope = rope.insert(6, emoji)

        Assertions.assertEquals(14, rope.totalChars)
        Assertions.assertEquals("🚀 ", rope.getText(6, 3))
        Assertions.assertEquals("🚀 World", rope.getText(6, 8))
    }

    @Test
    fun testDeepBalance() {
        var rope = Rope("")
        repeat(1000) {
            rope = rope.insert(0, "a")
        }

        Assertions.assertTrue(rope.totalChars == 1000) { "Tree is too deep: ${rope.maxLineLength}" }
    }
}