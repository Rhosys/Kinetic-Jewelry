package ch.rhosys.lyra.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Draws a thumb indicating scroll position; only visible once content overflows the viewport. */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 3.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f),
): Modifier =
    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        if (totalItems == 0 || visibleItems.isEmpty() || visibleItems.size >= totalItems) return@drawWithContent

        val itemHeight = size.height / totalItems
        val firstVisibleIndex = visibleItems.first().index
        val thumbOffsetY = firstVisibleIndex * itemHeight
        val thumbHeight = (visibleItems.size * itemHeight).coerceAtMost(size.height)

        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), thumbOffsetY),
            size = Size(width.toPx(), thumbHeight),
        )
    }
