package com.finalplayer.app.ui.components

// Selection Action Bar and Top Bar Components for Multi-Select Mode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen

@Composable
fun SelectionTopAppBar(
    totalCount: Int,
    selectedCount: Int,
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onInfoClick: () -> Unit,
    onMoreOptionsClick: (() -> Unit)? = null,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Select/Deselect All (dashed selection square)
            IconButton(
                onClick = onToggleSelectAll,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = if (isAllSelected) "إلغاء تحديد الكل" else "تحديد الكل",
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Left Action: Info icon (circle-i)
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "معلومات",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right side: Selected counter text (e.g. selected 25 / 1)
            Text(
                text = "selected $totalCount / $selectedCount",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Right side: Close 'X' button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إلغاء التحديد",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SelectionBottomActionBar(
    selectedCount: Int,
    onHideClick: () -> Unit,
    onShareClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMoveClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionItem(
                    icon = Icons.Default.Lock,
                    label = "إخفاء",
                    onClick = onHideClick
                )
                SelectionActionItem(
                    icon = Icons.Default.Share,
                    label = "مشاركة",
                    onClick = onShareClick
                )
                SelectionActionItem(
                    icon = Icons.Default.Edit,
                    label = "إعادة تسمية",
                    enabled = selectedCount == 1,
                    onClick = onRenameClick
                )
                SelectionActionItem(
                    icon = Icons.Default.Delete,
                    label = "حذف",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteClick
                )
                SelectionActionItem(
                    icon = Icons.Default.ContentCopy,
                    label = "نسخ",
                    onClick = onCopyClick
                )
                SelectionActionItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    label = "نقل",
                    onClick = onMoveClick
                )
                SelectionActionItem(
                    icon = Icons.Default.PlayArrow,
                    label = "FeedPlay",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onPlayClick
                )
            }
        }
    }
}

@Composable
fun SecureSelectionBottomActionBar(
    selectedCount: Int,
    onRestoreClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionItem(
                    icon = Icons.Default.LockOpen,
                    label = "استعادة للهاتف",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onRestoreClick
                )
                SelectionActionItem(
                    icon = Icons.Default.Share,
                    label = "مشاركة",
                    onClick = onShareClick
                )
                SelectionActionItem(
                    icon = Icons.Default.Delete,
                    label = "حذف نهائي",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteClick
                )
                SelectionActionItem(
                    icon = Icons.Default.PlayArrow,
                    label = "تشغيل",
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = onPlayClick
                )
            }
        }
    }
}

@Composable
private fun SelectionActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.38f)
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = effectiveTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = effectiveTint
        )
    }
}
