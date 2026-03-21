package io.github.numq.haskcore.feature.workspace.presentation.tab

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorkspaceTabs(
    documents: List<WorkspaceDocument>,
    activeDocumentPath: String?,
    selectDocument: (WorkspaceDocument) -> Unit,
    closeDocument: (WorkspaceDocument) -> Unit
) {
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    var isAreaHovered by remember { mutableStateOf(false) }

    var isScrollbarHovered by remember { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }

    val canScroll = listState.canScrollForward || listState.canScrollBackward

    val maxScrollbarThickness = 4.dp

    val scrollbarThickness by animateDpAsState(
        targetValue = when {
            isScrollbarHovered || isPressed -> maxScrollbarThickness

            else -> 2.dp
        }, animationSpec = tween()
    )

    val scrollbarAlpha by animateFloatAsState(
        targetValue = when {
            (isAreaHovered || isPressed) && canScroll -> 1f

            else -> 0f
        }, animationSpec = tween()
    )

    LaunchedEffect(activeDocumentPath) {
        activeDocumentPath?.let { path ->
            val index = documents.indexOfFirst { document -> document.path == path }

            if (index == -1) return@let

            val layoutInfo = listState.layoutInfo

            val visibleItems = layoutInfo.visibleItemsInfo

            val visibleItem = visibleItems.find { visibleItem -> visibleItem.index == index }

            val isFullyVisible = when (visibleItem) {
                null -> false

                else -> {
                    val itemStart = visibleItem.offset

                    val itemEnd = visibleItem.offset + visibleItem.size

                    val viewportStart = layoutInfo.viewportStartOffset

                    val viewportEnd = layoutInfo.viewportEndOffset

                    itemStart >= viewportStart && itemEnd <= viewportEnd
                }
            }

            if (!isFullyVisible) {
                val itemOffset = visibleItem?.offset ?: 0

                val viewportStart = layoutInfo.viewportStartOffset

                when {
                    (visibleItem != null && itemOffset < viewportStart) || (visibleItem == null && index < (visibleItems.firstOrNull()?.index
                        ?: 0)) -> listState.animateScrollToItem(index, scrollOffset = 0)

                    else -> {
                        val itemSize =
                            visibleItem?.size ?: (layoutInfo.viewportEndOffset / (visibleItems.size.coerceAtLeast(1)))

                        val targetOffset = layoutInfo.viewportEndOffset - itemSize

                        listState.animateScrollToItem(index, scrollOffset = -targetOffset)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
        .onPointerEvent(PointerEventType.Enter) {
            isAreaHovered = true
        }.onPointerEvent(PointerEventType.Exit) {
            isAreaHovered = false
        }.onPointerEvent(PointerEventType.Release) {
            isPressed = false
        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(maxScrollbarThickness).graphicsLayer {
            alpha = scrollbarAlpha
        }.onPointerEvent(PointerEventType.Enter) {
            isScrollbarHovered = true
        }.onPointerEvent(PointerEventType.Exit) {
            isScrollbarHovered = false
        }.onPointerEvent(PointerEventType.Press) {
            isPressed = true
        }.onPointerEvent(PointerEventType.Release) {
            isPressed = false
        }) {
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                style = ScrollbarStyle(
                    minimalHeight = 16.dp,
                    thickness = scrollbarThickness,
                    shape = RectangleShape,
                    hoverDurationMillis = 300,
                    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .2f),
                    hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = .8f)
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(35.dp).onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta

                val scrollAmount = (delta.y + delta.x) * 80f

                scope.launch { listState.scrollBy(scrollAmount) }
            }) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = true
            ) {
                items(items = documents, key = WorkspaceDocument::path, contentType = { it::class }) { document ->
                    WorkspaceTab(
                        document = document,
                        isActive = document.path == activeDocumentPath,
                        select = selectDocument,
                        close = closeDocument
                    )
                }
            }
        }
    }
}