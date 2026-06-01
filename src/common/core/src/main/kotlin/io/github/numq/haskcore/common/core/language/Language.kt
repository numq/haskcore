package io.github.numq.haskcore.common.core.language

sealed interface Language {
    companion object {
        fun fromExtension(path: String) = path.substringAfterLast(".").let { extension ->
            when (extension.lowercase()) {
                in Haskell.extensions -> Haskell

                else -> Undefined
            }
        }
    }

    val extensions: List<String>

    val guidelineColumn: Int?

    data object Undefined : Language {
        override val extensions = emptyList<String>()

        override val guidelineColumn = null
    }

    data object Haskell : Language {
        private const val HASKELL_EXTENSION_HS = "hs"

        private const val HASKELL_EXTENSION_LHS = "lhs"

        private const val HASKELL_GUIDELINE_COLUMN = 80

        override val extensions = listOf(HASKELL_EXTENSION_HS, HASKELL_EXTENSION_LHS)

        override val guidelineColumn = HASKELL_GUIDELINE_COLUMN
    }
}