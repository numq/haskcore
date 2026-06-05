package io.github.numq.haskcore.feature.editor.presentation.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.feature.editor.presentation.overlay.OverlayBox
import kotlin.math.roundToInt

@Composable
internal fun SuggestionPopup(
    suggestionsState: SuggestionsState.Visible,
    theme: EditorTheme,
    applySuggestion: (CodeSuggestion) -> Unit,
    dismiss: () -> Unit = {},
) {
    Popup(
        offset = IntOffset(x = suggestionsState.offset.x.roundToInt(), y = suggestionsState.offset.y.roundToInt()),
        properties = PopupProperties(
            focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
        ),
        onDismissRequest = dismiss
    ) {
        OverlayBox(
            modifier = Modifier.sizeIn(maxWidth = 512.dp, maxHeight = 256.dp),
            backgroundColor = Color(theme.overlayColorPalette.suggestionsBackgroundColor),
            borderColor = Color(theme.overlayColorPalette.suggestionsBorderColor)
        ) {
            val listState = rememberLazyListState()

            LaunchedEffect(suggestionsState.selectedIndex) {
                if (suggestionsState.suggestions.isNotEmpty()) {
                    val visibleInfo = listState.layoutInfo

                    val visibleItems = visibleInfo.visibleItemsInfo

                    val isVisible = visibleItems.any { visibleItem ->
                        visibleItem.index == suggestionsState.selectedIndex
                    }

                    if (!isVisible && visibleItems.isNotEmpty()) {
                        when {
                            suggestionsState.selectedIndex < visibleItems.first().index -> listState.animateScrollToItem(
                                suggestionsState.selectedIndex
                            )

                            else -> {
                                val viewportHeight = visibleInfo.viewportEndOffset - visibleInfo.viewportStartOffset

                                val itemHeight = visibleItems.last().size

                                listState.animateScrollToItem(
                                    suggestionsState.selectedIndex, scrollOffset = -(viewportHeight - itemHeight)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                itemsIndexed(suggestionsState.suggestions) { index, suggestion ->
                    val isSelected = index == suggestionsState.selectedIndex

                    Row(
                        modifier = Modifier.fillMaxWidth().background(
                            color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedBackgroundColor)

                                else -> Color(theme.overlayColorPalette.suggestionsBackgroundColor)
                            },
                        ).clickable {
                            applySuggestion(suggestion)
                        }.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = suggestion.label,
                            color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedTextColor)

                                else -> Color(theme.overlayColorPalette.suggestionsTextColor)
                            },
                        )
                        Text(
                            text = suggestion.kind.name.lowercase(),
                            color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedTextColor)

                                else -> Color(theme.overlayColorPalette.suggestionsTextColor)
                            },
                        )
                    }
                }
            }
        }
    }
}