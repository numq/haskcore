package io.github.numq.haskcore.feature.editor.core

import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection

data class Editor(
    val language: Language,
    val snapshot: TextSnapshot,
    val caret: Caret,
    val selection: Selection,
)