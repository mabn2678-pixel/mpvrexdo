package com.finalplayer.app.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState: SheetState,
    sortBy: String,
    sortAscending: Boolean,
    viewMode: String,
    layoutMode: String,
    visibleFields: Set<String>,
    onlyForFolderList: Boolean,
    showAudioFiles: Boolean,
    onDismiss: () -> Unit,
    onSortByChanged: (String) -> Unit,
    onSortAscendingChanged: (Boolean) -> Unit,
    onViewModeChanged: (String) -> Unit,
    onLayoutModeChanged: (String) -> Unit,
    onVisibleFieldsChanged: (Set<String>) -> Unit,
    onOnlyForFolderListChanged: (Boolean) -> Unit,
    onShowAudioFilesChanged: (Boolean) -> Unit
) {
    var isFieldsExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Title
            Text(
                text = "خيارات الفرز والعرض / Sort & View",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SORT BY SECTION
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val directionText = when (sortBy) {
                    "size" -> if (sortAscending) "▲ الأصغر حجمًا" else "▼ الأكبر حجمًا"
                    "date" -> if (sortAscending) "▲ الأقدم" else "▼ الأحدث"
                    "duration" -> if (sortAscending) "▲ الأقصر مدة" else "▼ الأطول مدة"
                    else -> if (sortAscending) "▲ أ - ي (A-Z)" else "▼ ي - أ (Z-A)"
                }

                Surface(
                    onClick = { onSortAscendingChanged(!sortAscending) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = directionText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "الفرز حسب / Sort by",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sort Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortOptionButton(
                    label = "الحجم",
                    icon = Icons.Default.SwapVert,
                    isSelected = sortBy == "size",
                    onClick = { onSortByChanged("size") },
                    modifier = Modifier.weight(1f)
                )
                SortOptionButton(
                    label = "التاريخ",
                    icon = Icons.Default.CalendarToday,
                    isSelected = sortBy == "date",
                    onClick = { onSortByChanged("date") },
                    modifier = Modifier.weight(1f)
                )
                SortOptionButton(
                    label = "المدة",
                    icon = Icons.Default.Schedule,
                    isSelected = sortBy == "duration",
                    onClick = { onSortByChanged("duration") },
                    modifier = Modifier.weight(1f)
                )
                SortOptionButton(
                    label = "الاسم",
                    icon = Icons.Default.Title,
                    isSelected = sortBy == "title",
                    onClick = { onSortByChanged("title") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // LAYOUT & VIEW MODE SECTION
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Layout sub-section (Grid, List)
                    Column(
                        modifier = Modifier.weight(0.42f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "التصميم",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SquareIconButton(
                                label = "شبكة",
                                icon = Icons.Default.GridView,
                                isSelected = layoutMode == "grid",
                                onClick = { onLayoutModeChanged("grid") }
                            )
                            SquareIconButton(
                                label = "قائمة",
                                icon = Icons.AutoMirrored.Filled.List,
                                isSelected = layoutMode == "list",
                                onClick = { onLayoutModeChanged("list") }
                            )
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(60.dp)
                            .padding(horizontal = 4.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // View Mode sub-section (Library, Tree, Folder)
                    Column(
                        modifier = Modifier.weight(0.58f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "نمط العرض",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SquareIconButton(
                                label = "المكتبة",
                                icon = Icons.Default.VideoLibrary,
                                isSelected = viewMode == "library",
                                onClick = { onViewModeChanged("library") }
                            )
                            SquareIconButton(
                                label = "شجرة",
                                icon = Icons.Default.AccountTree,
                                isSelected = viewMode == "tree",
                                onClick = { onViewModeChanged("tree") }
                            )
                            SquareIconButton(
                                label = "مجلدات",
                                icon = Icons.Default.Folder,
                                isSelected = viewMode == "folder",
                                onClick = { onViewModeChanged("folder") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // FILTERS SECTION
            // ==========================================
            Text(
                text = "الفلاتر / Filters",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض الملفات الصوتية",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = showAudioFiles,
                    onCheckedChange = { onShowAudioFilesChanged(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تطبيق الفرز على مجلدات فقط",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = onlyForFolderList,
                    onCheckedChange = { onOnlyForFolderListChanged(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // FIELDS SECTION (COLLAPSIBLE)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFieldsExpanded = !isFieldsExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rotateAngle by animateFloatAsState(
                    targetValue = if (isFieldsExpanded) 180f else 0f,
                    label = "arrow"
                )
                Text(
                    text = "الحقول الظاهرة / Visible Fields",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.rotate(rotateAngle)
                )
            }

            AnimatedVisibility(visible = isFieldsExpanded) {
                val availableFields = listOf(
                    "Path",
                    "Folder Size",
                    "Total Media",
                    "Full Name",
                    "Total Duration",
                    "Resolution",
                    "File Size",
                    "Date",
                    "Progress Bar"
                )

                CustomFlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 10.dp
                ) {
                    availableFields.forEach { field ->
                        val isSelected = visibleFields.contains(field)
                        FieldChipTag(
                            label = field,
                            isSelected = isSelected,
                            onClick = {
                                val updated = visibleFields.toMutableSet()
                                if (isSelected) updated.remove(field) else updated.add(field)
                                onVisibleFieldsChanged(updated)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SortOptionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}

@Composable
private fun SquareIconButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}

@Composable
private fun FieldChipTag(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}

@Composable
private fun CustomFlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 8.dp,
    crossAxisSpacing: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val mainAxisSpacingPx = mainAxisSpacing.roundToPx()
        val crossAxisSpacingPx = crossAxisSpacing.roundToPx()

        class RowInfo(
            val placeables: MutableList<Placeable> = mutableListOf(),
            var width: Int = 0,
            var height: Int = 0
        )

        val rows = mutableListOf<RowInfo>()
        var currentRow = RowInfo()

        for (measurable in measurables) {
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            if (currentRow.placeables.isNotEmpty() && currentRow.width + mainAxisSpacingPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = RowInfo()
            }
            if (currentRow.placeables.isNotEmpty()) {
                currentRow.width += mainAxisSpacingPx
            }
            currentRow.placeables.add(placeable)
            currentRow.width += placeable.width
            currentRow.height = maxOf(currentRow.height, placeable.height)
        }
        if (currentRow.placeables.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = rows.sumOf { it.height } + ((rows.size - 1).coerceAtLeast(0) * crossAxisSpacingPx)

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            for (row in rows) {
                var x = constraints.maxWidth - row.width
                for (placeable in row.placeables) {
                    placeable.placeRelative(x, y)
                    x += placeable.width + mainAxisSpacingPx
                }
                y += row.height + crossAxisSpacingPx
            }
        }
    }
}
