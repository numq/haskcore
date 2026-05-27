package io.github.numq.haskcore.feature.editor.presentation.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion

@Composable
internal fun CompletionPopup(
    offset: Offset,
    suggestions: List<CodeSuggestion>,
    theme: EditorTheme,
    onSuggestionClick: (CodeSuggestion) -> Unit,
) {
    when {
        suggestions.isEmpty() -> return

        else -> {
            val density = LocalDensity.current

            val xPx = with(density) { offset.x.dp.roundToPx() }

            val yPx = with(density) { offset.y.dp.roundToPx() }

            Popup(
                offset = IntOffset(xPx, yPx), properties = PopupProperties(
                    focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier.sizeIn(maxWidth = 300.dp, maxHeight = 200.dp)
                        .background(Color(theme.backgroundColorPalette.backgroundColor))
                        .border(1.dp, Color(theme.gutterColorPalette.separatorColor)).padding(vertical = 4.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onSuggestionClick(suggestion)
                                }.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = suggestion.label,
                                    color = Color(theme.codeAreaColorPalette.textColor),
                                )
                                Text(
                                    text = suggestion.kind.name.lowercase(),
                                    color = Color(theme.codeAreaColorPalette.textColor).copy(alpha = .5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}