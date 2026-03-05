package io.github.numq.haskcore.core.text

sealed interface TextOperation {
    val data: Data

    data class User(override val data: Data) : TextOperation

    data class System(override val data: Data) : TextOperation

    sealed interface Data {
        sealed interface Single : Data {
            data class Insert(val position: TextPosition, val text: String) : Single

            data class Replace(val range: TextRange, val text: String) : Single

            data class Delete(val range: TextRange) : Single
        }

        data class Batch(val operations: List<Single>) : Data
    }
}