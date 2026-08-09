package com.finalplayer.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val extension = remember(currentName) {
        val lastDot = currentName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < currentName.length - 1) {
            currentName.substring(lastDot)
        } else {
            ""
        }
    }

    val nameWithoutExtension = remember(currentName, extension) {
        if (extension.isNotEmpty()) {
            currentName.removeSuffix(extension)
        } else {
            currentName
        }
    }

    var textState by remember { mutableStateOf(nameWithoutExtension) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndConfirm() {
        val trimmed = textState.trim()
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        when {
            trimmed.isEmpty() -> {
                errorMessage = "لا يمكن أن يكون اسم الملف فارغاً"
            }
            trimmed.any { it in invalidChars } -> {
                errorMessage = "اسم الملف لا يمكن أن يحتوي على أحرف خاصة (/ \\ : * ? \" < > |)"
            }
            else -> {
                errorMessage = null
                val newFullName = if (extension.isNotEmpty()) "$trimmed$extension" else trimmed
                onConfirm(newFullName)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعادة تسمية الملف",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "أدخل الاسم الجديد للملف:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        errorMessage = null
                    },
                    suffix = if (extension.isNotEmpty()) {
                        { Text(text = extension, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else null,
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndConfirm() },
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("إعادة تسمية")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_cancel_button")
            ) {
                Text("إلغاء")
            }
        }
    )
}
