package io.github.numq.haskcore.common.presentation.font

import org.jetbrains.skia.Typeface

interface FontManager : AutoCloseable {
    fun loadFont(fileName: String): Typeface
}