package io.github.numq.haskcore.feature.log.presentation.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.numq.haskcore.common.core.log.Log
import io.github.numq.haskcore.common.presentation.container.Container
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun LogView(projectScope: Scope, handleError: (Throwable) -> Unit) {
    val scope = rememberCoroutineScope()

    val feature = koinInject<LogFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is LogEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val listState = rememberLazyListState()

    val isAtBottom by remember { derivedStateOf { !listState.canScrollForward } }

    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    val infoColor = MaterialTheme.colorScheme.primary

    val errorColor = MaterialTheme.colorScheme.error

    Container {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Logs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            feature.execute(LogCommand.Clear)
                        }
                    }, modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f), state = listState, contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(items = state.logs, key = Log::id) { log ->
                    val (markerColor, level) = when (log) {
                        is Log.Info -> infoColor to "INFO"

                        is Log.Warning -> Color(0xFFE65100) to "WARN"

                        is Log.Error -> errorColor to "ERROR"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                            .padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.width(3.dp).fillMaxHeight()
                                .background(markerColor, shape = MaterialTheme.shapes.small)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = markerColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )

                            if (log is Log.Error) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .3f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${log.className}\n${log.stackTrace}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}