package com.finalplayer.app.ui.securefolder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PinPad(
    enteredDigits: Int,
    maxDigits: Int = 4,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // مؤشرات الأرقام
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(maxDigits) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (index < enteredDigits)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }

        // رسالة الخطأ
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(8.dp))

        // شبكة الأرقام 3×4
        val keys = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2)  // -1 = فارغ, -2 = حذف
        )

        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        -1 -> {
                            if (onBiometricClick != null) {
                                IconButton(
                                    onClick = onBiometricClick,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "البصمة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(72.dp))
                            }
                        }
                        -2 -> {
                            // زر الحذف
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "حذف",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        else -> {
                            // رقم
                            Surface(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = enteredDigits < maxDigits,
                                        onClick = { onDigit(key) }
                                    ),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        key.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
