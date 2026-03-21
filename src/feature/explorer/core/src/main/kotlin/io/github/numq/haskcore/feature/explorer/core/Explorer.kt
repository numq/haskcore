package io.github.numq.haskcore.feature.explorer.core

data class Explorer(val expandedPaths: List<String> = emptyList(), val index: Int = 0, val offset: Int = 0)