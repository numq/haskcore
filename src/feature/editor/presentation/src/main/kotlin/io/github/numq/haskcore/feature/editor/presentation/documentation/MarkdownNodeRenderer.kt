package io.github.numq.haskcore.feature.editor.presentation.documentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

@Composable
internal fun MarkdownNodeRenderer(
    rootNode: ASTNode,
    content: String,
    backgroundColor: Color,
    textColor: Color,
    navigate: (String) -> Unit,
) {
    when (rootNode.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            val validChildren = rootNode.children.filter { child ->
                child.type != MarkdownTokenTypes.WHITE_SPACE && child.type != MarkdownTokenTypes.EOL
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                validChildren.forEachIndexed { index, child ->
                    MarkdownNodeRenderer(
                        rootNode = child,
                        content = content,
                        backgroundColor = backgroundColor,
                        textColor = textColor,
                        navigate = navigate
                    )

                    val isSignature =
                        index == 0 && (child.type == MarkdownElementTypes.CODE_FENCE || child.type == MarkdownElementTypes.CODE_BLOCK)

                    val hasMoreContent = index < validChildren.lastIndex

                    val nextIsNotDivider =
                        hasMoreContent && validChildren[index + 1].type != MarkdownTokenTypes.HORIZONTAL_RULE

                    if (isSignature && hasMoreContent && nextIsNotDivider) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = textColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        MarkdownElementTypes.LIST_ITEM -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rootNode.children.forEach { child ->
                MarkdownNodeRenderer(
                    rootNode = child,
                    content = content,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    navigate = navigate
                )
            }
        }

        MarkdownElementTypes.PARAGRAPH -> {
            val annotatedString = buildAnnotatedString {
                appendMarkdownInline(rootNode, content, backgroundColor, textColor, navigate)
            }

            Text(text = annotatedString, color = textColor, fontSize = 13.sp)
        }

        MarkdownElementTypes.CODE_BLOCK, MarkdownElementTypes.CODE_FENCE -> {
            val rawCode = rootNode.getTextInNode(content).toString()

            val cleanCode =
                rawCode.replace(Regex("^```[a-zA-Z]*\\n?"), "").replace(Regex("\\n?```$"), "")
                    .trim()

            Box(
                modifier = Modifier.fillMaxWidth().background(backgroundColor.copy(alpha = .5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = cleanCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp), color = textColor.copy(alpha = 0.2f)
        )

        MarkdownTokenTypes.TEXT -> Text(
            text = rootNode.getTextInNode(content).toString(), color = textColor, fontSize = 13.sp
        )

        MarkdownTokenTypes.WHITE_SPACE -> Unit

        else -> rootNode.children.forEach { child ->
            MarkdownNodeRenderer(
                rootNode = child,
                content = content,
                backgroundColor = backgroundColor,
                textColor = textColor,
                navigate = navigate
            )
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownInline(
    node: ASTNode,
    content: String,
    backgroundColor: Color,
    textColor: Color,
    navigate: (String) -> Unit,
) {
    val text = node.getTextInNode(allFileText = content).toString()

    when (node.type) {
        MarkdownTokenTypes.TEXT, MarkdownTokenTypes.WHITE_SPACE -> append(text)

        MarkdownElementTypes.CODE_SPAN -> {
            val codeText = " ${text.trim('`')} "

            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = backgroundColor,
                    background = textColor
                )
            ) {
                append(codeText)
            }
        }

        MarkdownElementTypes.INLINE_LINK, MarkdownElementTypes.AUTOLINK -> {
            val linkTextNode = node.children.find { child ->
                child.type == MarkdownElementTypes.LINK_TEXT
            }

            val linkDestNode = node.children.find { child ->
                child.type == MarkdownElementTypes.LINK_DESTINATION
            }

            val linkText = linkTextNode?.getTextInNode(content)?.toString()?.trim('[', ']') ?: text

            val linkDest = linkDestNode?.getTextInNode(content)?.toString()?.trim('<', '>') ?: ""

            val linkAnnotation = LinkAnnotation.Clickable(
                tag = linkDest, linkInteractionListener = {
                    navigate(linkDest)
                })

            pushLink(linkAnnotation)

            withStyle(
                SpanStyle(
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(linkText)
            }

            pop()
        }

        MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET, MarkdownTokenTypes.LPAREN, MarkdownTokenTypes.RPAREN, MarkdownElementTypes.LINK_DESTINATION, MarkdownElementTypes.LINK_TEXT -> Unit

        else -> node.children.forEach { child ->
            appendMarkdownInline(
                node = child,
                content = content,
                backgroundColor = backgroundColor,
                textColor = textColor,
                navigate = navigate
            )
        }
    }
}