package io.github.numq.haskcore.core.text

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

sealed interface TextEdit {
    val startByte: Int

    val oldEndByte: Int

    val newEndByte: Int

    val startPosition: TextPosition

    val oldEndPosition: TextPosition

    val newEndPosition: TextPosition

    val timestamp: Duration

    sealed interface Single : TextEdit {
        val oldText: String?

        val newText: String?

        data class Insert(
            override val startPosition: TextPosition,
            override val newEndPosition: TextPosition,
            val insertedText: String,
            override val startByte: Int,
            override val newEndByte: Int
        ) : Single {
            override val oldEndPosition = startPosition

            override val oldEndByte = startByte

            override val oldText = null

            override val newText = insertedText

            override val timestamp = System.nanoTime().nanoseconds
        }

        data class Delete(
            override val startPosition: TextPosition,
            override val oldEndPosition: TextPosition,
            val deletedText: String,
            override val startByte: Int,
            override val oldEndByte: Int
        ) : Single {
            override val newEndPosition = startPosition

            override val newEndByte = startByte

            override val oldText = deletedText

            override val newText = null

            override val timestamp = System.nanoTime().nanoseconds
        }

        data class Replace(
            override val startPosition: TextPosition,
            override val oldEndPosition: TextPosition,
            override val newEndPosition: TextPosition,
            override val oldText: String,
            override val newText: String,
            override val startByte: Int,
            override val oldEndByte: Int,
            override val newEndByte: Int
        ) : Single {
            override val timestamp = System.nanoTime().nanoseconds
        }
    }

    data class Batch(val edits: List<TextEdit>) : TextEdit {
        override val startByte = edits.minOfOrNull(TextEdit::startByte) ?: 0

        override val oldEndByte = edits.maxOfOrNull(TextEdit::oldEndByte) ?: 0

        override val newEndByte = edits.maxOfOrNull(TextEdit::newEndByte) ?: 0

        override val startPosition = edits.minByOrNull(TextEdit::startPosition)?.startPosition ?: TextPosition.ZERO

        override val oldEndPosition = edits.maxByOrNull(TextEdit::oldEndPosition)?.oldEndPosition ?: TextPosition.ZERO

        override val newEndPosition = edits.maxByOrNull(TextEdit::newEndPosition)?.newEndPosition ?: TextPosition.ZERO

        override val timestamp = System.nanoTime().nanoseconds
    }
}