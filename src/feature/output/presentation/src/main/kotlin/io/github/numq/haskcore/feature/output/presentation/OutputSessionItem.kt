package io.github.numq.haskcore.feature.output.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputLine
import io.github.numq.haskcore.feature.output.core.OutputSession

@Composable
internal fun OutputSessionItem(session: OutputSession) {
    val listState = rememberLazyListState()

    LaunchedEffect(session.lines.size) {
        if (session.lines.isNotEmpty()) {
            listState.animateScrollToItem(session.lines.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items = session.lines, key = OutputLine::id, contentType = { it::class }) { line ->
            OutputLineItem(line = line)
        }
    }
}