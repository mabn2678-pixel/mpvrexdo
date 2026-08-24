package com.finalplayer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.finalplayer.app.data.transfer.TransferProgress
import com.finalplayer.app.data.transfer.TransferType

@Composable
fun FileTransferProgressDialog(
    progress: TransferProgress?,
    onCancel: () -> Unit,
    onMoveToBackground: () -> Unit
) {
    if (progress == null || !progress.isRunning || progress.isBackground) return

    val actionTitle: String
    val actionVerb: String
    val iconVector: ImageVector
    val iconBgColor: Color
    val iconTintColor: Color
    val progressColor: Color

    when (progress.type) {
        TransferType.MOVE -> {
            actionTitle = "نقل الملفات"
            actionVerb = "جاري النقل"
            iconVector = Icons.Default.DriveFileMove
            iconBgColor = Color(0xFF388E3C).copy(alpha = 0.2f)
            iconTintColor = Color(0xFF81C784)
            progressColor = Color(0xFF4CAF50)
        }
        TransferType.COPY -> {
            actionTitle = "نسخ الملفات"
            actionVerb = "جاري النسخ"
            iconVector = Icons.Default.FileCopy
            iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            iconTintColor = MaterialTheme.colorScheme.primary
            progressColor = MaterialTheme.colorScheme.primary
        }
        TransferType.HIDE_TO_SECURE -> {
            actionTitle = "نقل إلى المجلد الآمن"
            actionVerb = "جاري التشفير والحماية"
            iconVector = Icons.Default.Lock
            iconBgColor = Color(0xFFF57C00).copy(alpha = 0.2f)
            iconTintColor = Color(0xFFFFB74D)
            progressColor = Color(0xFFFF9800)
        }
        TransferType.RESTORE_FROM_SECURE -> {
            actionTitle = "استعادة إلى الهاتف"
            actionVerb = "جاري فك الحماية والاستعادة"
            iconVector = Icons.Default.PhoneAndroid
            iconBgColor = Color(0xFF0097A7).copy(alpha = 0.2f)
            iconTintColor = Color(0xFF4DD0E1)
            progressColor = Color(0xFF00ACC1)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (progress.percentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 200),
        label = "transfer_progress_anim"
    )

    Dialog(
        onDismissRequest = { /* Prevent accidental outside dismiss while transferring */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E222B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header: Icon + Title + Type Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = iconBgColor,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = iconTintColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = actionTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$actionVerb...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Percentage Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = progressColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, progressColor.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = "${progress.percentage}%",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = progressColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Current File Name with high legibility
                        if (progress.currentFileName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = progress.currentFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Progress Indicator
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = progressColor,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Progress Stats Row: File Count (7/15) and Size (325 MB of 1.1 GB)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Files counter: e.g. 7 / 15
                                Text(
                                    text = "${progress.currentFileIndex} / ${progress.totalFileCount} ملف",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.85f)
                                )

                                // Data transferred: e.g. 325 MB of 1.1 GB
                                Text(
                                    text = "${progress.transferredSizeFormatted} من ${progress.totalSizeFormatted}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Action Buttons: Cancel and Move to Background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF5350)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إلغاء",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Background Transfer Button
                            OutlinedButton(
                                onClick = onMoveToBackground,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "متابعة في الخلفية",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
