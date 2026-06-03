package io.github.numq.haskcore.feature.workspace.core

import io.github.numq.haskcore.common.core.language.Language

data class WorkspaceDocument(val path: String, val name: String, val language: Language)