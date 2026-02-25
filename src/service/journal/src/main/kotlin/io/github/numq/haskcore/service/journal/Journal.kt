package io.github.numq.haskcore.service.journal

import io.github.numq.haskcore.core.text.TextEdit

data class Journal(val edits: List<TextEdit> = emptyList(), val currentIndex: Int = -1)