package io.github.numq.haskcore.feature.editor.presentation.documentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.core.analysis.CodeDocumentation

@Composable
internal fun DocumentationPopup(
    offset: Offset,
    documentation: CodeDocumentation?,
    theme: EditorTheme,
    onDismissRequest: () -> Unit = {},
) {
    when {
        documentation == null || documentation.content.isBlank() -> return

        else -> {
            val density = LocalDensity.current

            val xPx = with(density) { offset.x.toDp().roundToPx() }

            val yPx = with(density) { offset.y.toDp().roundToPx() }

            Popup(
                offset = IntOffset(xPx, yPx), properties = PopupProperties(
                    focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
                ), onDismissRequest = onDismissRequest
            ) {
                Box(
                    modifier = Modifier.sizeIn(maxWidth = 450.dp, maxHeight = 300.dp)
                        .background(Color(theme.overlayColorPalette.tooltipBackgroundColor))
                        .border(1.dp, Color(theme.overlayColorPalette.tooltipBorderColor)).padding(8.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
                    ) {
                        Text(
                            text = documentation.content,
                            color = Color(theme.overlayColorPalette.tooltipTextColor),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}