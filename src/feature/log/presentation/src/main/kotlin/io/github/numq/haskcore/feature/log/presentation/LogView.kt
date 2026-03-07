package io.github.numq.haskcore.feature.log.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.core.log.Log
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun LogView(projectScope: Scope, handleError: (Throwable) -> Unit) {
    if (projectScope.closed) return

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

    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Surface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
        ) {
            items(items = state.logs, key = Log::timestamp, contentType = { it::class }) { log ->
                LogItem(log = log)
            }
        }
    }
}