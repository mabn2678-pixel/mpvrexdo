package com.finalplayer.app.subtitles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.finalplayer.app.player.core.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SubtitleFont(
    val id: String,
    val name: String,
    val fileName: String,
    val isBundled: Boolean = true,
    val isCustom: Boolean = false,
    val isSystemDefault: Boolean = false,
    val localFilePath: String? = null,
    val fileSize: Long = 0L
)

object SubtitleFontManager {
    private const val TAG = "SubtitleFontManager"
    private const val SUB_FONTS_DIR_NAME = "sub_fonts"

    // القائمة الافتراضية للخطوط المطابقة تماماً للصورة والمشهورة في الترجمة
    val DEFAULT_FONTS = listOf(
        SubtitleFont(
            id = "system_default",
            name = "خط النظام الافتراضي (System Default)",
            fileName = "",
            isBundled = false,
            isSystemDefault = true
        ),
        SubtitleFont(
            id = "b_arabic_style",
            name = "B Arabic Style",
            fileName = "b_arabic_style.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "bein_black",
            name = "BeIn Black",
            fileName = "bein_black.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "boutros_asma",
            name = "BoutrosAsma",
            fileName = "boutros_asma.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "dima_shekari",
            name = "DimaShekari",
            fileName = "dima_shekari.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "the_year_of_handicraft",
            name = "The Year Of Handicraft",
            fileName = "the_year_of_handicraft.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "takeaway_combo",
            name = "Takeaway Combo",
            fileName = "takeaway_combo.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "cairo",
            name = "Cairo (خط كايرو)",
            fileName = "cairo.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "tajawal",
            name = "Tajawal (خط تجوال)",
            fileName = "tajawal.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "almarai",
            name = "Almarai (خط المراعي)",
            fileName = "almarai.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "amiri",
            name = "Amiri (خط أميري الكلاسيكي)",
            fileName = "amiri.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "readex_pro",
            name = "Readex Pro (خط ريدكس برو)",
            fileName = "readex_pro.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "changa",
            name = "Changa (خط تشانغا)",
            fileName = "changa.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "el_messiri",
            name = "El Messiri (خط المسيري)",
            fileName = "el_messiri.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "alexandria",
            name = "Alexandria (خط الإسكندرية)",
            fileName = "alexandria.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "lalezar",
            name = "Lalezar (خط لاله زار العريض)",
            fileName = "lalezar.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "rubik",
            name = "Rubik (خط روبيك الحديث)",
            fileName = "rubik.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "kufam",
            name = "Kufam (خط كوفام العربي)",
            fileName = "kufam.ttf",
            isBundled = true
        ),
        SubtitleFont(
            id = "aref_ruqaa",
            name = "Aref Ruqaa (خط الرقعة)",
            fileName = "aref_ruqaa.ttf",
            isBundled = true
        )
    )

    /**
     * إرجاع المجلد المخصص لحفظ الخطوط في ذاكرة التطبيق الداخلية لضمان وصول MPV إليه دون قيود أذونات
     */
    fun getFontsDirectory(context: Context): File {
        val dir = File(context.filesDir, SUB_FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * استخراج كافة الخطوط المدمجة في assets إلى مجلد الخطوط الفعلي في التخزين الداخلي
     */
    fun ensureBundledFontsExtracted(context: Context) {
        val targetDir = getFontsDirectory(context)
        try {
            val assetFonts = context.assets.list(SUB_FONTS_DIR_NAME) ?: emptyArray()
            for (assetName in assetFonts) {
                if (assetName.endsWith(".ttf", ignoreCase = true) || assetName.endsWith(".otf", ignoreCase = true)) {
                    val targetFile = File(targetDir, assetName)
                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        try {
                            context.assets.open("$SUB_FONTS_DIR_NAME/$assetName").use { input ->
                                FileOutputStream(targetFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d(TAG, "Extracted font asset $assetName to ${targetFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to extract font asset $assetName", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking asset fonts", e)
        }
    }

    /**
     * جلب جميع الخطوط المتوفرة (المدمجة + المستوردة + الموجودة في مجلد مخصص إن وجد)
     */
    suspend fun getAllFonts(context: Context, customFolderPath: String? = null): List<SubtitleFont> = withContext(Dispatchers.IO) {
        ensureBundledFontsExtracted(context)
        val fontsDir = getFontsDirectory(context)
        val result = mutableListOf<SubtitleFont>()

        // 1. إضافة الخطوط الافتراضية
        for (defaultFont in DEFAULT_FONTS) {
            if (defaultFont.isSystemDefault) {
                result.add(defaultFont)
                continue
            }
            val localFile = File(fontsDir, defaultFont.fileName)
            val exists = localFile.exists()
            val size = if (exists) localFile.length() else 0L
            result.add(
                defaultFont.copy(
                    localFilePath = if (exists) localFile.absolutePath else null,
                    fileSize = size
                )
            )
        }

        // 2. البحث عن أي خطوط إضافية مستوردة في مجلد sub_fonts
        val internalFiles = fontsDir.listFiles() ?: emptyArray()
        val defaultFileNames = DEFAULT_FONTS.map { it.fileName.lowercase() }.toSet()
        for (file in internalFiles) {
            val nameLower = file.name.lowercase()
            if ((nameLower.endsWith(".ttf") || nameLower.endsWith(".otf")) && nameLower !in defaultFileNames) {
                val cleanTitle = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                    .split(' ')
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                result.add(
                    SubtitleFont(
                        id = "imported_${file.name}",
                        name = cleanTitle,
                        fileName = file.name,
                        isBundled = false,
                        isCustom = true,
                        localFilePath = file.absolutePath,
                        fileSize = file.length()
                    )
                )
            }
        }

        // 3. البحث في المجلد الخارجي المخصص إن تم تحديده
        if (!customFolderPath.isNullOrBlank()) {
            try {
                val customDir = File(customFolderPath)
                if (customDir.exists() && customDir.isDirectory) {
                    val customFiles = customDir.listFiles() ?: emptyArray()
                    for (file in customFiles) {
                        val nameLower = file.name.lowercase()
                        if (nameLower.endsWith(".ttf") || nameLower.endsWith(".otf")) {
                            // نسخ الخط إلى مجلد التطبيق الداخلي ليتمكن MPV من قراءته فوراً
                            val targetCopy = File(fontsDir, file.name)
                            if (!targetCopy.exists()) {
                                try {
                                    file.copyTo(targetCopy, overwrite = true)
                                } catch (_: Exception) {}
                            }
                            val cleanTitle = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                            if (result.none { it.fileName.equals(file.name, ignoreCase = true) }) {
                                result.add(
                                    SubtitleFont(
                                        id = "custom_folder_${file.name}",
                                        name = cleanTitle,
                                        fileName = file.name,
                                        isBundled = false,
                                        isCustom = true,
                                        localFilePath = targetCopy.absolutePath,
                                        fileSize = file.length()
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning custom fonts folder $customFolderPath", e)
            }
        }

        result
    }

    /**
     * استيراد ملف خط مخصص (.ttf أو .otf) من منتقي الملفات
     */
    suspend fun importFontFromUri(context: Context, uri: Uri): Result<SubtitleFont> = withContext(Dispatchers.IO) {
        try {
            var fileName = "custom_font_${System.currentTimeMillis()}.ttf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }

            val targetDir = getFontsDirectory(context)
            val destFile = File(targetDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("تعذر فتح ملف الخط"))

            val cleanName = destFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
            val font = SubtitleFont(
                id = "imported_${destFile.name}",
                name = cleanName,
                fileName = destFile.name,
                isBundled = false,
                isCustom = true,
                localFilePath = destFile.absolutePath,
                fileSize = destFile.length()
            )
            Result.success(font)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing font from uri", e)
            Result.failure(e)
        }
    }

    /**
     * تطبيق الخط المختار فورياً وبشكل فعلي على مشغل MPV
     */
    fun applyFontToMPV(context: Context, fontName: String, fontFileName: String? = null) {
        ensureBundledFontsExtracted(context)
        val fontsDir = getFontsDirectory(context)

        try {
            // 1. تحديد مجلد الخطوط لـ MPV
            MPVLib.setOptionString("sub-fonts-dir", fontsDir.absolutePath)
            MPVLib.setPropertyString("sub-fonts-dir", fontsDir.absolutePath)

            // 2. تطبيق اسم الخط أو اسم ملف الخط
            if (fontName.isNotBlank() && !fontName.contains("System Default", ignoreCase = true) && !fontName.contains("خط النظام", ignoreCase = true)) {
                // إيجاد ملف الخط الدقيق إن وجد
                val matchingFont = DEFAULT_FONTS.find { 
                    it.name.equals(fontName, ignoreCase = true) || it.fileName.equals(fontFileName, ignoreCase = true) || it.fileName.equals(fontName, ignoreCase = true)
                }
                
                val targetFontValue = when {
                    !fontFileName.isNullOrBlank() -> fontFileName
                    matchingFont != null -> matchingFont.fileName
                    else -> fontName
                }

                // نمرر الاسم واسم الملف
                MPVLib.setPropertyString("sub-font", targetFontValue)
                MPVLib.setOptionString("sub-font", targetFontValue)
                MPVLib.setPropertyString("osd-font", targetFontValue)
                MPVLib.setOptionString("osd-font", targetFontValue)
            } else {
                MPVLib.setPropertyString("sub-font", "")
                MPVLib.setOptionString("sub-font", "")
                MPVLib.setPropertyString("osd-font", "")
                MPVLib.setOptionString("osd-font", "")
            }

            // 3. فرض تطبيق الخط على كافة الترجمات (Override ASS/SSA/SRT)
            MPVLib.setPropertyString("sub-ass-override", "force")
            MPVLib.setOptionString("sub-ass-override", "force")

            // 4. إعادة تحميل الترجمة وتحديث عرضها فوراً
            try {
                MPVLib.command("sub-reload")
            } catch (_: Throwable) {}

            Log.d(TAG, "Successfully applied subtitle font '$fontName' to MPV from dir: ${fontsDir.absolutePath}")
        } catch (e: Throwable) {
            Log.e(TAG, "Error applying font to MPV", e)
        }
    }

    /**
     * توليد FontFamily لـ Compose لعرض معاينة واقعية لكل خط في الواجهة
     */
    fun getComposeFontFamily(context: Context, font: SubtitleFont): FontFamily {
        if (font.isSystemDefault || font.fileName.isBlank()) {
            return FontFamily.Default
        }
        val file = if (font.localFilePath != null) File(font.localFilePath) else File(getFontsDirectory(context), font.fileName)
        if (file.exists()) {
            try {
                return FontFamily(Font(file))
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to load Font from file ${file.absolutePath}: ${e.message}")
            }
        }
        return FontFamily.Default
    }
}
