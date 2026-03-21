package io.github.numq.haskcore.feature.editor.core.guideline

data class Guideline(val column: Int) {
    companion object {
        private const val HASKELL_GUIDELINE_COLUMN = 80

        val Haskell = Guideline(column = HASKELL_GUIDELINE_COLUMN)
    }
}