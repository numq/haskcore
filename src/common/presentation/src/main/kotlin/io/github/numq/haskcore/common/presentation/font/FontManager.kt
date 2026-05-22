package io.github.numq.haskcore.common.presentation.font

import arrow.core.Either

interface FontManager : AutoCloseable {
    private companion object {
        const val DEFAULT_SIZE = 13f

        const val DEFAULT_LINE_SPACING = 1.2f
    }

    suspend fun loadFont(
        fileName: String, size: Float = DEFAULT_SIZE, lineSpacing: Float = DEFAULT_LINE_SPACING,
    ): Either<Throwable, Font>
}