package io.github.numq.haskcore.service.lsp.hover

import io.github.numq.haskcore.common.core.text.TextRange

data class LspHover(val content: String, val range: TextRange? = null)