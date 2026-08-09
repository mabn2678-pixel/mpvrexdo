package com.finalplayer.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()

        class RowInfo(
            val placeables: MutableList<Placeable> = mutableListOf(),
            var width: Int = 0,
            var height: Int = 0
        )

        val rows = mutableListOf<RowInfo>()
        var currentRow = RowInfo()

        for (measurable in measurables) {
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            if (currentRow.placeables.isNotEmpty() && currentRow.width + horizontalSpacingPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = RowInfo()
            }
            if (currentRow.placeables.isNotEmpty()) {
                currentRow.width += horizontalSpacingPx
            }
            currentRow.placeables.add(placeable)
            currentRow.width += placeable.width
            currentRow.height = maxOf(currentRow.height, placeable.height)
        }
        if (currentRow.placeables.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = rows.sumOf { it.height } + ((rows.size - 1).coerceAtLeast(0) * verticalSpacingPx)
        val totalWidth = constraints.maxWidth

        layout(totalWidth, totalHeight) {
            var y = 0
            for (row in rows) {
                var x = 0
                for (placeable in row.placeables) {
                    placeable.placeRelative(x, y)
                    x += placeable.width + horizontalSpacingPx
                }
                y += row.height + verticalSpacingPx
            }
        }
    }
}
