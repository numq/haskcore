package io.github.numq.haskcore.platform.font

import org.jetbrains.skia.Typeface

class MonoFont(typeface: Typeface, size: Float, lineSpacing: Float) : CustomFont(
    typeface = typeface, size = size, lineSpacing = lineSpacing
)