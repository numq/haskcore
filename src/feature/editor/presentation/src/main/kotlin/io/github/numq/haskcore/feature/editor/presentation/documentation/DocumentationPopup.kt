package io.github.numq.haskcore.feature.editor.presentation.documentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material.RichText
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.presentation.overlay.OverlayBox
import kotlin.math.roundToInt

@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun DocumentationPopup(
    documentationState: DocumentationState.Visible, theme: EditorTheme, dismiss: () -> Unit = {},
) {
    Popup(
        offset = IntOffset(x = documentationState.offset.x.roundToInt(), y = documentationState.offset.y.roundToInt()),
        properties = PopupProperties(
            focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true
        ),
        onDismissRequest = dismiss
    ) {
        val scrollState = rememberScrollState()

        val richTextState = rememberRichTextState()

        LaunchedEffect(documentationState.documentation.content) {
            richTextState.setMarkdown(documentationState.documentation.content)
        }

        OverlayBox(
            modifier = Modifier.sizeIn(maxWidth = 512.dp, maxHeight = 256.dp),
            backgroundColor = Color(theme.overlayColorPalette.documentationBackgroundColor),
            borderColor = Color(theme.overlayColorPalette.documentationTextColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(8.dp)) {
                RichText(
                    state = richTextState,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(theme.overlayColorPalette.documentationTextColor),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}