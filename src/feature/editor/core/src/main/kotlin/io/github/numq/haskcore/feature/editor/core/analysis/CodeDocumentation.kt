package io.github.numq.haskcore.feature.editor.core.analysis

import io.github.numq.haskcore.common.core.text.TextRange

data class CodeDocumentation(val content: String, val range: TextRange? = null)