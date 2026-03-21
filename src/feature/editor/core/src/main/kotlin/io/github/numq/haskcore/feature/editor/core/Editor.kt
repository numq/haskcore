package io.github.numq.haskcore.feature.editor.core

import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.guideline.Guideline
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax

data class Editor(
    val snapshot: TextSnapshot,
    val caret: Caret,
    val selection: Selection,
    val guideline: Guideline?,
    val analysis: Analysis?,
    val syntax: Syntax?,
)