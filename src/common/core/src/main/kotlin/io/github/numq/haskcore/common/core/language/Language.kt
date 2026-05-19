package io.github.numq.haskcore.common.core.language

sealed interface Language {
    val column: Int?

    data object Undefined : Language {
        override val column = null
    }

    data object Haskell : Language {
        private const val HASKELL_GUIDELINE_COLUMN = 80

        override val column = HASKELL_GUIDELINE_COLUMN
    }
}