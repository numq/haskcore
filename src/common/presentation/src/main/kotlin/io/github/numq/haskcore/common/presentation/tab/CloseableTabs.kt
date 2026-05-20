package io.github.numq.haskcore.common.presentation.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun <T> CloseableTabs(
    modifier: Modifier = Modifier,
    items: List<T>,
    activeItem: T?,
    title: String? = null,
    getItemName: (T) -> String,
    select: (T) -> Unit,
    close: (T) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 10.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f).horizontalScroll(state = scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                CloseableTab(
                    title = getItemName(item),
                    isSelected = item == activeItem,
                    select = { select(item) },
                    close = { close(item) })
            }
        }
    }
}