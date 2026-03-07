package io.github.numq.haskcore.feature.editor.core.caret

import io.github.numq.haskcore.core.text.TextPosition

data class Caret(val position: TextPosition) {
    companion object {
        val ZERO = Caret(position = TextPosition.ZERO)
    }
}