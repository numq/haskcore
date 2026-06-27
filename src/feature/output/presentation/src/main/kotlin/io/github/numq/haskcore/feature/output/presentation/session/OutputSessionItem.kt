package io.github.numq.haskcore.feature.output.presentation.session

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputLine
import io.github.numq.haskcore.feature.output.core.OutputSession
import io.github.numq.haskcore.feature.output.presentation.line.OutputLineItem

@Composable
internal fun OutputSessionItem(modifier: Modifier, session: OutputSession) {
    val listState = rememberLazyListState()

    LaunchedEffect(session.lines.size) {
        if (session.lines.isNotEmpty()) {
            listState.animateScrollToItem(session.lines.size - 1)
        }
    }

    LazyColumn(modifier = modifier.padding(4.dp), state = listState) {
        items(items = session.lines, key = OutputLine::id) { line ->
            OutputLineItem(line = line)
        }
    }
}