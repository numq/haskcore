package io.github.numq.haskcore.service.journal

import io.github.numq.haskcore.common.core.text.TextEdit

data class Journal(val edits: List<TextEdit> = emptyList())