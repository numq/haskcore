package io.github.numq.haskcore.feature.editor.presentation.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.numq.haskcore.common.presentation.overlay.popup.PopupBox
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import kotlin.math.roundToInt

@Composable
internal fun SuggestionPopup(
    suggestionsState: SuggestionsState.Visible,
    theme: EditorTheme,
    applySuggestion: (CodeSuggestion) -> Unit,
    dismiss: () -> Unit = {},
) {
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()

    val labelStyle = MaterialTheme.typography.bodySmall

    val kindStyle = MaterialTheme.typography.labelSmall

    val maxWidth = remember(suggestionsState.suggestions) {
        suggestionsState.suggestions.maxOfOrNull { suggestion ->
            val labelSize = textMeasurer.measure(
                text = suggestion.label, style = labelStyle
            ).size.width

            val kindSize = textMeasurer.measure(
                text = suggestion.kind.name.lowercase(), style = kindStyle.copy(
                    fontSize = 11.sp, fontStyle = FontStyle.Italic
                )
            ).size.width

            with(density) {
                (labelSize + 16.dp.toPx() + kindSize + 8.dp.toPx() * 2 + 6.dp.toPx() * 2).toDp()
            }
        }?.coerceAtMost(512.dp) ?: 200.dp
    }

    Popup(
        offset = IntOffset(x = suggestionsState.offset.x.roundToInt(), y = suggestionsState.offset.y.roundToInt()),
        properties = PopupProperties(
            focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
        ),
        onDismissRequest = dismiss
    ) {
        PopupBox(
            modifier = Modifier.width(maxWidth).heightIn(max = 256.dp),
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

            LazyColumn(state = listState) {
                itemsIndexed(
                    items = suggestionsState.suggestions,
                    key = { _, suggestion -> suggestion.label }) { index, suggestion ->
                    val isSelected = index == suggestionsState.selectedIndex

                    Row(
                        modifier = Modifier.fillMaxWidth().background(
                            color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedBackgroundColor)

                                else -> Color(theme.overlayColorPalette.suggestionsBackgroundColor)
                            },
                        ).clickable {
                            applySuggestion(suggestion)
                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion.label, modifier = Modifier.weight(1f), color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedTextColor)

                                else -> Color(theme.overlayColorPalette.suggestionsTextColor)
                            }, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = suggestion.kind.name.lowercase(), modifier = Modifier.background(
                                color = when {
                                    isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedBackgroundColor).copy(
                                        alpha = .5f
                                    )

                                    else -> Color(theme.overlayColorPalette.suggestionsTextColor).copy(alpha = .1f)
                                }, shape = RoundedCornerShape(4.dp)
                            ).padding(horizontal = 6.dp, vertical = 2.dp), color = when {
                                isSelected -> Color(theme.overlayColorPalette.suggestionsSelectedTextColor).copy(alpha = .7f)

                                else -> Color(theme.overlayColorPalette.suggestionsTextColor).copy(alpha = .6f)
                            }, fontSize = 11.sp, fontStyle = FontStyle.Italic, maxLines = 1
                        )
                    }
                }
            }
        }
    }
}