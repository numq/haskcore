package io.github.numq.haskcore.feature.editor.presentation.documentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.numq.haskcore.common.presentation.overlay.popup.PopupBox
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DocumentationPopup(
    documentationState: DocumentationState.Visible,
    theme: EditorTheme,
    mouseEnter: () -> Unit,
    mouseExit: () -> Unit,
    navigate: (String) -> Unit,
    dismiss: () -> Unit = {},
) {
    val flavour = remember {
        CommonMarkFlavourDescriptor()
    }

    val rootNode = remember(documentationState.documentation.content) {
        MarkdownParser(flavour).buildMarkdownTreeFromString(documentationState.documentation.content)
    }

    Popup(
        offset = IntOffset(x = documentationState.offset.x.roundToInt(), y = documentationState.offset.y.roundToInt()),
        properties = PopupProperties(
            focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true
        ),
        onDismissRequest = dismiss
    ) {
        val scrollState = rememberScrollState()

        PopupBox(
            modifier = Modifier.onPointerEvent(PointerEventType.Enter) {
                mouseEnter()
            }.onPointerEvent(PointerEventType.Exit) {
                mouseExit()
            }.widthIn(max = 512.dp).heightIn(max = 256.dp),
            backgroundColor = Color(theme.overlayColorPalette.documentationBackgroundColor),
            borderColor = Color(theme.overlayColorPalette.documentationBorderColor)
        ) {
            SelectionContainer {
                Column(modifier = Modifier.widthIn(max = 512.dp).verticalScroll(scrollState).padding(8.dp)) {
                    MarkdownNodeRenderer(
                        rootNode = rootNode,
                        content = documentationState.documentation.content,
                        backgroundColor = Color(theme.overlayColorPalette.documentationBackgroundColor),
                        textColor = Color(theme.overlayColorPalette.documentationTextColor),
                        navigate = navigate
                    )
                }
            }
        }
    }
}