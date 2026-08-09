package com.finalplayer.app.ui.securefolder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel
import kotlinx.coroutines.delay

@Composable
fun PinInputDialog(
    maxDigits: Int = 4,
    onPinEntered: (String) -> SecureFolderViewModel.UnlockResult,
    onBiometric: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var pinDigits by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var lockCountdown by remember { mutableLongStateOf(0L) }

    // Countdown timer لو مقفول
    LaunchedEffect(isLocked) {
        if (isLocked) {
            while (lockCountdown > 0) {
                delay(1000L)
                lockCountdown--
            }
            isLocked = false
            errorMessage = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "المجلد الآمن",
                    style = MaterialTheme.typography.titleLarge
                )

                if (isLocked) {
                    Text(
                        "محاولات كثيرة. انتظر ${lockCountdown}s",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                PinPad(
                    enteredDigits = pinDigits.length,
                    maxDigits = maxDigits,
                    onDigit = { digit ->
                        if (!isLocked && pinDigits.length < maxDigits) {
                            pinDigits += digit.toString()
                            errorMessage = null
                            if (pinDigits.length == maxDigits) {
                                // محاولة التحقق
                                when (val result = onPinEntered(pinDigits)) {
                                    is SecureFolderViewModel.UnlockResult.Success ->
                                        onDismiss()
                                    is SecureFolderViewModel.UnlockResult.WrongPin -> {
                                        errorMessage = if (result.attemptsLeft > 0)
                                            "PIN غير صحيح — ${result.attemptsLeft} محاولات متبقية"
                                        else "PIN غير صحيح"
                                        pinDigits = ""
                                    }
                                    is SecureFolderViewModel.UnlockResult.Locked -> {
                                        isLocked = true
                                        lockCountdown = result.secondsRemaining
                                        pinDigits = ""
                                    }
                                }
                            }
                        }
                    },
                    onDelete = {
                        if (pinDigits.isNotEmpty())
                            pinDigits = pinDigits.dropLast(1)
                        errorMessage = null
                    },
                    onBiometricClick = onBiometric,
                    errorMessage = errorMessage
                )

                // زر البصمة لو متاح
                onBiometric?.let {
                    TextButton(onClick = it) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Text("استخدم البصمة")
                        }
                    }
                }
            }
        }
    }
}
