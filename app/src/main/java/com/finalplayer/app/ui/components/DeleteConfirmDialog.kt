package com.finalplayer.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun DeleteConfirmDialog(
    itemCount: Int = 1,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val titleText = if (itemCount > 1) "حذف الملفات؟" else "حذف الملف؟"
    val bodyText = if (itemCount > 1) {
        "سيتم حذف $itemCount ملفات بشكل نهائي ولا يمكن التراجع عن هذا الإجراء."
    } else {
        "سيتم حذف هذا الملف بشكل نهائي ولا يمكن التراجع عن هذا الإجراء."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("delete_confirm_button")
            ) {
                Text("حذف")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("delete_cancel_button")
            ) {
                Text("إلغاء")
            }
        }
    )
}
