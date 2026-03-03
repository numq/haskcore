package io.github.numq.haskcore.service.language.semantic

import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange

internal class HlsSemanticDecoder : SemanticDecoder {
    override suspend fun decode(data: List<Int>, legend: List<LegendType>) = either {
        ensure(data.size % 5 == 0) {
            IllegalArgumentException("Invalid semantic tokens data size: ${data.size}")
        }

        val tokens = ArrayList<SemanticToken>(data.size / 5)

        var currentLine = 0

        var currentStartChar = 0

        for (i in data.indices step 5) {
            val deltaLine = data[i]

            val deltaStartChar = data[i + 1]

            val length = data[i + 2]

            val typeIndex = data[i + 3]

            val modifiers = data[i + 4]

            currentLine += deltaLine

            currentStartChar = when (deltaLine) {
                0 -> currentStartChar + deltaStartChar

                else -> deltaStartChar
            }

            val legendType = legend.getOrElse(typeIndex) { LegendType.VARIABLE }

            tokens.add(
                SemanticToken(
                    range = TextRange(
                        start = TextPosition(currentLine, currentStartChar),
                        end = TextPosition(currentLine, currentStartChar + length)
                    ), type = legendType, modifiers = modifiers
                )
            )
        }

        tokens
    }
}