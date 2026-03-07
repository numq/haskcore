package io.github.numq.haskcore.feature.editor.presentation.overlay.completion

import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterIcon

internal data class AutocompleteItem(
    val label: String,
    val detail: String? = null,
    val documentation: String? = null,
    val icon: GutterIcon.IconType? = null,
    val insertText: String,
    val kind: CompletionItemKind
)