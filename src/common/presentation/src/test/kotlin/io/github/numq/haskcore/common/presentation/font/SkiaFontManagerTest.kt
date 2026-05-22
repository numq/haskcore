package io.github.numq.haskcore.common.presentation.font

import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class SkiaFontManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fontManager: SkiaFontManager

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fontManager = SkiaFontManager()
    }

    @AfterEach
    fun tearDown() {
        fontManager.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `should load font successfully`() = runTest {
        val result = fontManager.loadFont(
            fileName = FontResources.MONO, size = 14f, lineSpacing = 1.5f
        )

        assertTrue(result.isRight(), "Font should be loaded successfully")

        result.onRight { font: Font ->
            assertNotNull(font)
            assertEquals(14f, font.size)
            assertEquals(1.5f, font.lineSpacing)
        }
    }

    @Test
    fun `should cache typeface for same file name`() = runTest {
        val result1 = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)
        val result2 = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())

        val font1 = result1.getOrElse { error("Should not fail") }
        val font2 = result2.getOrElse { error("Should not fail") }

        assertSame(font1.typeface, font2.typeface, "Typeface should be cached and reused")
    }

    @Test
    fun `should create different fonts for different sizes`() = runTest {
        val result1 = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)
        val result2 = fontManager.loadFont(FontResources.MONO, 18f, 1.5f)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())

        val font1 = result1.getOrElse { error("Should not fail") }
        val font2 = result2.getOrElse { error("Should not fail") }

        assertEquals(14f, font1.size)
        assertEquals(18f, font2.size)
        assertNotSame(font1, font2, "Different font instances for different sizes")
        assertSame(font1.typeface, font2.typeface, "Typeface should be reused")
    }

    @Test
    fun `should create different fonts for different line spacing`() = runTest {
        val result1 = fontManager.loadFont(FontResources.MONO, 14f, 1.0f)
        val result2 = fontManager.loadFont(FontResources.MONO, 14f, 2.0f)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())

        val font1 = result1.getOrElse { error("Should not fail") }
        val font2 = result2.getOrElse { error("Should not fail") }

        assertEquals(1.0f, font1.lineSpacing)
        assertEquals(2.0f, font2.lineSpacing)
        assertNotSame(font1, font2, "Different font instances for different line spacing")
        assertSame(font1.typeface, font2.typeface, "Typeface should be reused")
    }

    @Test
    fun `should throw exception when loading from closed manager`() = runTest {
        fontManager.close()

        val result = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is IllegalStateException)
            assertEquals("Font manager is closed", error.message)
        }
    }

    @Test
    fun `should throw exception when resource not found`() = runTest {
        val result = fontManager.loadFont("non_existent_font.ttf", 14f, 1.5f)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is IllegalStateException)
            assertTrue(error.message?.contains("Font resource not found") == true)
        }
    }

    @Test
    fun `should throw exception for empty file name`() = runTest {
        val result = fontManager.loadFont("", 14f, 1.5f)

        assertTrue(result.isLeft())
    }

    @Test
    fun `should handle zero size gracefully`() = runTest {
        val result = fontManager.loadFont(FontResources.MONO, 0f, 1.5f)

        if (result.isRight()) {
            result.onRight { font: Font ->
                assertEquals(0f, font.size)
            }
        }
    }

    @Test
    fun `should handle negative size gracefully`() = runTest {
        val result = fontManager.loadFont(FontResources.MONO, -1f, 1.5f)

        assertNotNull(result)
    }

    @Test
    fun `should close manager without errors`() = runTest {
        fontManager.loadFont(FontResources.MONO, 14f, 1.5f)

        assertDoesNotThrow("Should close without errors") {
            fontManager.close()
        }
    }

    @Test
    fun `should handle double close gracefully`() = runTest {
        fontManager.close()

        assertDoesNotThrow("Double close should not throw") {
            fontManager.close()
        }
    }

    @Test
    fun `should not load font after close`() = runTest {
        val resultBefore = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)
        assertTrue(resultBefore.isRight(), "Should load before close")

        fontManager.close()

        val resultAfter = fontManager.loadFont(FontResources.MONO, 14f, 1.5f)
        assertTrue(resultAfter.isLeft(), "Should fail after close")
    }

    @Test
    fun `should cache typeface across different font instances`() = runTest {
        val font1 = fontManager.loadFont(FontResources.MONO, 12f, 1.0f).getOrElse { error("") }
        val font2 = fontManager.loadFont(FontResources.MONO, 14f, 1.5f).getOrElse { error("") }
        val font3 = fontManager.loadFont(FontResources.MONO, 16f, 2.0f).getOrElse { error("") }

        assertSame(font1.typeface, font2.typeface, "All should share same Typeface")
        assertSame(font2.typeface, font3.typeface, "All should share same Typeface")
        assertNotSame(font1, font2, "Font instances should be different")
        assertNotSame(font2, font3, "Font instances should be different")
    }
}