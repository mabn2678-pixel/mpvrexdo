package com.finalplayer.app.ui.settings.tabs

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.finalplayer.app.ui.settings.components.SettingsSectionHeader

@Composable
fun AdvancedSettingsTab() {
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsSectionHeader("لغة التطبيق") }
        item {
            ListItem(
                headlineContent = { Text("اللغة") },
                supportingContent = { Text("إعداد النظام الافتراضي") },
                modifier = Modifier.clickable {
                    // فتح إعدادات اللغة
                    context.startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
                }
            )
        }

        item { SettingsSectionHeader("النسخ الاحتياطي والاستعادة") }
        item {
            ListItem(
                headlineContent = { Text("تصدير الإعدادات") },
                supportingContent = { Text("تصدير الإعدادات إلى ملف XML") },
                trailingContent = {
                    Icon(Icons.Default.Upload, null)
                },
                modifier = Modifier.clickable { /* TODO: export */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("استيراد الإعدادات") },
                supportingContent = { Text("استيراد الإعدادات من ملف XML") },
                trailingContent = {
                    Icon(Icons.Default.Download, null)
                },
                modifier = Modifier.clickable { /* TODO: import */ }
            )
        }

        item { SettingsSectionHeader("جذر التخزين") }
        item {
            ListItem(
                headlineContent = { Text("اختيار موقع تخزين الإعدادات") },
                supportingContent = { Text("انقر للاختيار - ينشئ المجلدات الفرعية Subtitles...") },
                trailingContent = {
                    Icon(Icons.Default.Folder, null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable { /* SAF folder picker */ }
            )
        }

        item { SettingsSectionHeader("إعدادات MPV") }
        item {
            var showMpvConf by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("إعداد MPV") },
                supportingContent = { Text("تعديل mpv.conf مباشرةً") },
                modifier = Modifier.clickable { showMpvConf = true }
            )
            if (showMpvConf) {
                // TODO: مرحلة لاحقة
            }
        }
    }
}
