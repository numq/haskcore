package io.github.numq.haskcore.platform.font

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FontManagerTest {
    @Test
    fun `should load and cache font`() {
        val manager = FontManager()

        val font1 = manager.loadFont(FontResources.MONO)
        val font2 = manager.loadFont(FontResources.MONO)

        assertNotNull(font1)
        assertSame(font1, font2, "Font should be cached and reused")

        manager.close()
    }

    @Test
    fun `should throw exception when loading from closed manager`() {
        val manager = FontManager()
        manager.close()

        assertThrows<IllegalStateException> {
            manager.loadFont(FontResources.MONO)
        }
    }

    @Test
    fun `should throw exception if resource not found`() {
        val manager = FontManager()

        assertThrows<IllegalStateException> {
            manager.loadFont("non_existent_font.ttf")
        }

        manager.close()
    }
}