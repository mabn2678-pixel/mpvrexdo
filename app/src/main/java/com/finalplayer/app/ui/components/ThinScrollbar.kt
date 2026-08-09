package com.finalplayer.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.thinScrollbar(
    state: LazyListState,
    width: Dp = 3.dp,
    color: Color = Color.White.copy(alpha = 0.4f)
): Modifier = this.drawWithContent {
    drawContent()

    val totalItems = state.layoutInfo.totalItemsCount
    if (totalItems <= 1) return@drawWithContent

    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return@drawWithContent

    val viewportHeight = state.layoutInfo.viewportEndOffset.toFloat()
    val thumbHeight = (viewportHeight / totalItems)
        .coerceAtLeast(40.dp.toPx())
        .coerceAtMost(viewportHeight)

    val scrollFraction = state.firstVisibleItemIndex.toFloat() / totalItems
    val thumbY = scrollFraction * (viewportHeight - thumbHeight)

    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), thumbY),
        size = Size(width.toPx(), thumbHeight),
        cornerRadius = CornerRadius(width.toPx() / 2)
    )
}
