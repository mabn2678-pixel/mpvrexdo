package com.finalplayer.app.ui.securefolder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finalplayer.app.ui.securefolder.components.PinPad
import com.finalplayer.app.utils.BiometricHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderSetupScreen(
    viewModel: SecureFolderViewModel = koinViewModel(),
    onSetupComplete: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }  // 0=choose, 1=enter, 2=confirm, 3=biometric
    var enteredPin by remember { mutableStateOf("") }
    var pinDigits by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعداد المجلد الآمن") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Progress indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            when (step) {
                // الخطوة 1: اختيار طريقة الحماية
                0 -> {
                    Text("اختر طريقة الحماية",
                        style = MaterialTheme.typography.headlineSmall)

                    // PIN Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { step = 1 },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Pin, null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("رمز PIN",
                                    style = MaterialTheme.typography.titleMedium)
                                Text("رقم سري من 4 إلى 6 أرقام",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }

                // الخطوة 2: إدخال PIN
                1 -> {
                    Text("أدخل رمز PIN الجديد",
                        style = MaterialTheme.typography.headlineSmall)
                    Text("4 إلى 6 أرقام",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    PinPad(
                        enteredDigits = pinDigits.length,
                        maxDigits = 6,
                        onDigit = { digit ->
                            if (pinDigits.length < 6) {
                                pinDigits += digit.toString()
                                errorMessage = null
                            }
                        },
                        onDelete = {
                            if (pinDigits.isNotEmpty())
                                pinDigits = pinDigits.dropLast(1)
                        },
                        errorMessage = errorMessage
                    )

                    Button(
                        onClick = {
                            if (pinDigits.length >= 4) {
                                enteredPin = pinDigits
                                pinDigits = ""
                                step = 2
                            } else {
                                errorMessage = "PIN يجب أن يكون 4 أرقام على الأقل"
                            }
                        },
                        enabled = pinDigits.length >= 4,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("التالي") }
                }

                // الخطوة 3: تأكيد PIN
                2 -> {
                    Text("أكّد رمز PIN",
                        style = MaterialTheme.typography.headlineSmall)

                    PinPad(
                        enteredDigits = pinDigits.length,
                        maxDigits = enteredPin.length,
                        onDigit = { digit ->
                            if (pinDigits.length < enteredPin.length) {
                                pinDigits += digit.toString()
                                errorMessage = null
                            }
                        },
                        onDelete = {
                            if (pinDigits.isNotEmpty())
                                pinDigits = pinDigits.dropLast(1)
                        },
                        errorMessage = errorMessage
                    )

                    Button(
                        onClick = {
                            if (pinDigits == enteredPin) {
                                viewModel.setupPin(pinDigits)
                                step = 3
                            } else {
                                errorMessage = "رمز PIN غير متطابق"
                                pinDigits = ""
                            }
                        },
                        enabled = pinDigits.length == enteredPin.length,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تأكيد") }
                }

                // الخطوة 4: البصمة (اختياري)
                3 -> {
                    Icon(Icons.Default.Fingerprint, null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary)

                    Text("تفعيل بصمة الإصبع؟",
                        style = MaterialTheme.typography.headlineSmall)

                    Text(
                        "يمكنك استخدام بصمة الإصبع بدلاً من رمز PIN للوصول السريع",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (BiometricHelper.canUseBiometric(context)) {
                        Button(
                            onClick = {
                                viewModel.enableBiometric(true)
                                onSetupComplete()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تفعيل البصمة") }
                    }

                    TextButton(
                        onClick = onSetupComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تخطي") }
                }
            }
        }
    }
}
