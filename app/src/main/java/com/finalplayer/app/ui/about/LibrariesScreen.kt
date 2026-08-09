package com.finalplayer.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class LibraryInfo(
    val name: String,
    val mavenCoordinate: String,
    val description: String,
    val license: String
)

val libraryList = listOf(
    LibraryInfo("Jetpack Compose", "androidx.compose.ui:ui", "إطار عمل واجهة المستخدم الحديث للاندرويد", "Apache 2.0"),
    LibraryInfo("Material 3", "androidx.compose.material3:material3", "مكونات وتصميم الماتيريال 3", "Apache 2.0"),
    LibraryInfo("Navigation Compose", "androidx.navigation:navigation-compose", "التنقل بين الشاشات بطريقة معتمدة على النوع", "Apache 2.0"),
    LibraryInfo("Koin Dependency Injection", "io.insert-koin:koin-androidx-compose", "حقن التبعيات الخفيف والسريع لـ Kotlin", "Apache 2.0"),
    LibraryInfo("Room Database", "androidx.room:room-runtime", "قاعدة بيانات محلية سريعة مبنية على SQLite", "Apache 2.0"),
    LibraryInfo("SMBJ", "com.hierynomus:smbj", "عميل بروتوكول SMB2/SMB3 للوصول لمشاركات الشبكة", "Apache 2.0"),
    LibraryInfo("Commons Net", "commons-net:commons-net", "مكتبة التعامل مع بروتوكولات الشبكة مثل FTP", "Apache 2.0"),
    LibraryInfo("Kotlinx Serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json", "تسلسل البيانات وحفظ الكائنات كـ JSON", "Apache 2.0"),
    LibraryInfo("Accompanist Permissions", "com.google.accompanist:accompanist-permissions", "إدارة صلاحيات الأندرويد بسلاسة مع Compose", "Apache 2.0"),
    LibraryInfo("AndroidX Media3 / Media", "androidx.media3:media3-exoplayer", "مشغل الوسائط المتعددة ونظام التشغيل", "Apache 2.0"),
    LibraryInfo("Seeker", "dev.shreyaspatil:seeker", "شريط تقدّم مخصص للتطبيقات الوسائط", "Apache 2.0")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المكتبات المستخدمة / Used Libraries") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(libraryList) { lib ->
                LibraryCard(library = lib)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LibraryCard(library: LibraryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = library.mavenCoordinate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = library.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الرخصة: ${library.license}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
