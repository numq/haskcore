package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.feature.explorer.core.ExplorerTree

data class ExplorerState(val explorerTree: ExplorerTree, val selectedPath: String? = null)