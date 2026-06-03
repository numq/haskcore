package io.github.numq.haskcore.feature.editor.presentation.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
internal fun SuggestionPopup(
    offset: Offset,
    suggestions: List<CodeSuggestion>,
    selectedIndex: Int,
    theme: EditorTheme,
    onSuggestionClick: (CodeSuggestion) -> Unit,
) {
    when {
        suggestions.isEmpty() -> return

        else -> {
            val density = LocalDensity.current

            val xPx = with(density) { offset.x.dp.roundToPx() }

            val yPx = with(density) { offset.y.dp.roundToPx() }

            val listState = rememberLazyListState()

            LaunchedEffect(selectedIndex) {
                if (suggestions.isNotEmpty()) {
                    val visibleInfo = listState.layoutInfo

                    val visibleItems = visibleInfo.visibleItemsInfo

                    val isVisible = visibleItems.any { visibleItem ->
                        visibleItem.index == selectedIndex
                    }

                    if (!isVisible && visibleItems.isNotEmpty()) {
                        when {
                            selectedIndex < visibleItems.first().index -> listState.animateScrollToItem(selectedIndex)

                            else -> {
                                val viewportHeight = visibleInfo.viewportEndOffset - visibleInfo.viewportStartOffset

                                val itemHeight = visibleItems.last().size

                                listState.animateScrollToItem(
                                    selectedIndex, scrollOffset = -(viewportHeight - itemHeight)
                                )
                            }
                        }
                    }
                }
            }

            Popup(
                offset = IntOffset(xPx, yPx), properties = PopupProperties(
                    focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier.sizeIn(maxWidth = 300.dp, maxHeight = 200.dp)
                        .background(Color(theme.overlayColorPalette.suggestionsBackgroundColor))
                        .border(1.dp, Color(theme.overlayColorPalette.suggestionsBorderColor)).padding(vertical = 4.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                        itemsIndexed(suggestions) { index, suggestion ->
                            val isSelected = index == selectedIndex

                            Row(
                                modifier = Modifier.fillMaxWidth().background(
                                    color = when {
                                        isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedBackgroundColor)

                                        else -> Color(theme.overlayColorPalette.suggestionsBackgroundColor)
                                    },
                                ).clickable {
                                    onSuggestionClick(suggestion)
                                }.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
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
    }
}