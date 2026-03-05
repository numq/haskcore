package io.github.numq.haskcore.core.text

import io.github.numq.haskcore.core.timestamp.Timestamp

sealed interface TextEdit {
    val data: Data

    val revision: Long

    data class User(override val data: Data, override val revision: Long) : TextEdit

    data class System(override val data: Data, override val revision: Long) : TextEdit

    fun invert() = when (this) {
        is User -> User(data = data.invert(), revision = revision)

        is System -> System(data = data.invert(), revision = revision)
    }

    sealed interface Data {
        val startByte: Int

        val oldEndByte: Int

        val newEndByte: Int

        val startPosition: TextPosition

        val oldEndPosition: TextPosition

        val newEndPosition: TextPosition

        val timestamp: Timestamp

        fun invert(): Data = when (this) {
            is Single.Insert -> Single.Delete(
                startPosition = startPosition,
                oldEndPosition = newEndPosition,
                deletedText = insertedText,
                startByte = startByte,
                oldEndByte = newEndByte
            )

            is Single.Replace -> Single.Replace(
                startPosition = startPosition,
                oldEndPosition = newEndPosition,
                newEndPosition = oldEndPosition,
                oldText = newText,
                newText = oldText,
                startByte = startByte,
                oldEndByte = newEndByte,
                newEndByte = oldEndByte
            )

            is Single.Delete -> Single.Insert(
                startPosition = startPosition,
                newEndPosition = oldEndPosition,
                insertedText = deletedText,
                startByte = startByte,
                newEndByte = oldEndByte
            )

            is Batch -> Batch(singles = singles.mapNotNull { single ->
                single.invert() as? Single
            }.reversed())
        }

        sealed interface Single : Data {
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

                override val timestamp = Timestamp.now()
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
                override val timestamp = Timestamp.now()
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

                override val timestamp = Timestamp.now()
            }
        }

        data class Batch(val singles: List<Single>) : Data {
            init {
                require(singles.isNotEmpty()) { "Batch edit cannot be empty" }
            }

            override val startByte = singles.first().startByte

            override val startPosition = singles.first().startPosition

            override val oldEndByte = singles.last().oldEndByte

            override val oldEndPosition = singles.last().oldEndPosition

            override val newEndByte = singles.last().newEndByte

            override val newEndPosition = singles.last().newEndPosition

            override val timestamp = singles.first().timestamp
        }
    }
}