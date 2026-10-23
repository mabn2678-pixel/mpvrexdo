package com.finalplayer.app.subtitles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.system.Os
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
    val familyName: String = name,
    val isBundled: Boolean = true,
    val isCustom: Boolean = false,
    val isSystemDefault: Boolean = false,
    val localFilePath: String? = null,
    val fileSize: Long = 0L
)

object SubtitleFontManager {
    private const val TAG = "SubtitleFontManager"
    private const val SUB_FONTS_DIR_NAME = "fonts"

    // القائمة الافتراضية للخطوط المطابقة تماماً للصورة والمشهورة في الترجمة العربية
    val DEFAULT_FONTS = listOf(
        SubtitleFont(
            id = "system_default",
            name = "خط النظام الافتراضي (System Default)",
            fileName = "",
            familyName = "",
            isBundled = false,
            isSystemDefault = true
        ),
        SubtitleFont(
            id = "b_arabic_style",
            name = "B Arabic Style",
            fileName = "b_arabic_style.ttf",
            familyName = "B Arabic Style",
            isBundled = true
        ),
        SubtitleFont(
            id = "bein_black",
            name = "BeIn Black",
            fileName = "bein_black.ttf",
            familyName = "BeIn Black",
            isBundled = true
        ),
        SubtitleFont(
            id = "boutros_asma",
            name = "BoutrosAsma",
            fileName = "boutros_asma.ttf",
            familyName = "BoutrosAsma",
            isBundled = true
        ),
        SubtitleFont(
            id = "dima_shekari",
            name = "DimaShekari",
            fileName = "dima_shekari.ttf",
            familyName = "DimaShekari",
            isBundled = true
        ),
        SubtitleFont(
            id = "the_year_of_handicraft",
            name = "The Year Of Handicraft",
            fileName = "the_year_of_handicraft.ttf",
            familyName = "The Year Of Handicraft",
            isBundled = true
        ),
        SubtitleFont(
            id = "takeaway_combo",
            name = "Takeaway Combo",
            fileName = "takeaway_combo.ttf",
            familyName = "Takeaway Combo",
            isBundled = true
        ),
        SubtitleFont(
            id = "cairo",
            name = "Cairo (خط كايرو)",
            fileName = "cairo.ttf",
            familyName = "Cairo",
            isBundled = true
        ),
        SubtitleFont(
            id = "tajawal",
            name = "Tajawal (خط تجوال)",
            fileName = "tajawal.ttf",
            familyName = "Tajawal",
            isBundled = true
        ),
        SubtitleFont(
            id = "amiri",
            name = "Amiri (الخط الأميري)",
            fileName = "amiri.ttf",
            familyName = "Amiri",
            isBundled = true
        ),
        SubtitleFont(
            id = "almarai",
            name = "Almarai (خط المراعي)",
            fileName = "almarai.ttf",
            familyName = "Almarai",
            isBundled = true
        ),
        SubtitleFont(
            id = "changa",
            name = "Changa (خط تشانغا)",
            fileName = "changa.ttf",
            familyName = "Changa",
            isBundled = true
        ),
        SubtitleFont(
            id = "readex_pro",
            name = "Readex Pro",
            fileName = "readex_pro.ttf",
            familyName = "Readex Pro",
            isBundled = true
        ),
        SubtitleFont(
            id = "el_messiri",
            name = "El Messiri (المسيري)",
            fileName = "el_messiri.ttf",
            familyName = "El Messiri",
            isBundled = true
        ),
        SubtitleFont(
            id = "alexandria",
            name = "Alexandria (الإسكندرية)",
            fileName = "alexandria.ttf",
            familyName = "Alexandria",
            isBundled = true
        ),
        SubtitleFont(
            id = "lalezar",
            name = "Lalezar (لاليزار)",
            fileName = "lalezar.ttf",
            familyName = "Lalezar",
            isBundled = true
        ),
        SubtitleFont(
            id = "rubik",
            name = "Rubik (روبيك)",
            fileName = "rubik.ttf",
            familyName = "Rubik Light",
            isBundled = true
        ),
        SubtitleFont(
            id = "kufam",
            name = "Kufam (خط كوفام)",
            fileName = "kufam.ttf",
            familyName = "Kufam",
            isBundled = true
        ),
        SubtitleFont(
            id = "aref_ruqaa",
            name = "Aref Ruqaa (عارف رقعة)",
            fileName = "aref_ruqaa.ttf",
            familyName = "Aref Ruqaa",
            isBundled = true
        )
    )

    /**
     * المجلد الافتراضي لخطوط الترجمة في ذاكرة التطبيق الداخلية (MPV standard ~~/fonts)
     */
    fun getFontsDirectory(context: Context): File {
        val dir = File(context.filesDir, SUB_FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * استخراج جميع الخطوط المدمجة في assets إلى مجلد التطبيق وتهيئة ملف fontconfig ومكتبة MPV
     */
    fun ensureBundledFontsExtracted(context: Context) {
        val fontsDir = getFontsDirectory(context)
        val legacySubFontsDir = File(context.filesDir, "sub_fonts")
        if (!legacySubFontsDir.exists()) {
            legacySubFontsDir.mkdirs()
        }

        try {
            val assetList = context.assets.list("sub_fonts") ?: emptyArray()
            for (fileName in assetList) {
                if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                    val destFile = File(fontsDir, fileName)
                    val legacyFile = File(legacySubFontsDir, fileName)
                    if (!destFile.exists() || destFile.length() == 0L) {
                        context.assets.open("sub_fonts/$fileName").use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (!legacyFile.exists() || legacyFile.length() == 0L) {
                        try {
                            destFile.copyTo(legacyFile, overwrite = true)
                        } catch (_: Throwable) {}
                    }
                }
            }

            // إنشاء ملف fonts.conf لتهيئة Fontconfig تلقائياً داخل MPV
            setupFontconfigFile(context, fontsDir)
            setupMpvConfigFile(context, fontsDir)

            Log.d(TAG, "Bundled fonts extracted successfully to: ${fontsDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting bundled fonts", e)
        }
    }

    private fun setupFontconfigFile(context: Context, fontsDir: File) {
        try {
            val cacheFontDir = File(context.cacheDir, "fontconfig")
            if (!cacheFontDir.exists()) cacheFontDir.mkdirs()

            val fontsConfFile = File(context.filesDir, "fonts.conf")
            val confXml = """
                <?xml version="1.0"?>
                <!DOCTYPE fontconfig SYSTEM "fonts.dtd">
                <fontconfig>
                    <dir>/system/fonts</dir>
                    <dir>${fontsDir.absolutePath}</dir>
                    <cachedir>${cacheFontDir.absolutePath}</cachedir>
                    <config>
                        <rescan>
                            <int>30</int>
                        </rescan>
                    </config>
                </fontconfig>
            """.trimIndent()

            fontsConfFile.writeText(confXml)

            // ضبط متغيرات البيئة لـ Fontconfig
            try {
                Os.setenv("FONTCONFIG_FILE", fontsConfFile.absolutePath, true)
                Os.setenv("FONTCONFIG_PATH", context.filesDir.absolutePath, true)
            } catch (_: Throwable) {}
        } catch (e: Throwable) {
            Log.e(TAG, "Error writing fonts.conf", e)
        }
    }

    private fun setupMpvConfigFile(context: Context, fontsDir: File) {
        try {
            val mpvConf = File(context.filesDir, "mpv.conf")
            val content = """
                sub-fonts-dir=${fontsDir.absolutePath}
                sub-font-provider=fontconfig
                sub-ass-override=force
            """.trimIndent()
            mpvConf.writeText(content)
        } catch (_: Throwable) {}
    }

    /**
     * استرداد قائمة الخطوط الكاملة (المدمجة + المستوردة + من مجلد خارجي)
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
            val exists = localFile.exists() && localFile.length() > 0
            result.add(
                defaultFont.copy(
                    localFilePath = if (exists) localFile.absolutePath else null,
                    fileSize = if (exists) localFile.length() else 0L
                )
            )
        }

        // 2. البحث عن أي خطوط إضافية مستوردة في مجلد التطبيق
        val internalFiles = fontsDir.listFiles() ?: emptyArray()
        for (file in internalFiles) {
            val nameLower = file.name.lowercase()
            if (nameLower.endsWith(".ttf") || nameLower.endsWith(".otf")) {
                if (result.any { it.fileName.equals(file.name, ignoreCase = true) }) {
                    continue
                }
                val familyName = FontNameExtractor.extractFamilyName(file)
                result.add(
                    SubtitleFont(
                        id = "imported_${file.name}",
                        name = familyName,
                        fileName = file.name,
                        familyName = familyName,
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
                            val targetCopy = File(fontsDir, file.name)
                            if (!targetCopy.exists()) {
                                try {
                                    file.copyTo(targetCopy, overwrite = true)
                                } catch (_: Exception) {}
                            }
                            val familyName = FontNameExtractor.extractFamilyName(file)
                            if (result.none { it.fileName.equals(file.name, ignoreCase = true) }) {
                                result.add(
                                    SubtitleFont(
                                        id = "custom_folder_${file.name}",
                                        name = familyName,
                                        fileName = file.name,
                                        familyName = familyName,
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

            val familyName = FontNameExtractor.extractFamilyName(destFile)
            val font = SubtitleFont(
                id = "imported_${destFile.name}",
                name = familyName,
                fileName = destFile.name,
                familyName = familyName,
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
            MPVLib.setOptionString("sub-font-provider", "fontconfig")

            val isSystemDefault = fontName.isBlank() ||
                    fontName.contains("System Default", ignoreCase = true) ||
                    fontName.contains("خط النظام", ignoreCase = true)

            if (!isSystemDefault) {
                // إيجاد عائلة الخط الحقيقية الدقيقة
                val matchingFont = DEFAULT_FONTS.find { 
                    it.name.equals(fontName, ignoreCase = true) ||
                    it.fileName.equals(fontFileName, ignoreCase = true) ||
                    it.fileName.equals(fontName, ignoreCase = true) ||
                    it.familyName.equals(fontName, ignoreCase = true) ||
                    it.id.equals(fontName, ignoreCase = true)
                }

                val targetFamilyName = matchingFont?.familyName ?: fontName

                // تعطيل خطوط الحاوية المدمجة واستبدالها بخط المستخدم المختار
                MPVLib.setPropertyString("embeddedfonts", "no")
                MPVLib.setOptionString("embeddedfonts", "no")

                // تطبيق اسم الخط على ترجمات SRT و VTT
                MPVLib.setPropertyString("sub-font", targetFamilyName)
                MPVLib.setOptionString("sub-font", targetFamilyName)

                // تطبيق اسم الخط على نصوص الواجهة OSD
                MPVLib.setPropertyString("osd-font", targetFamilyName)
                MPVLib.setOptionString("osd-font", targetFamilyName)

                // فرض تطبيق الخط على ترجمات ASS و SSA المعقدة
                MPVLib.setPropertyString("sub-ass-override", "force")
                MPVLib.setOptionString("sub-ass-override", "force")
                MPVLib.setPropertyString("sub-ass-force-style", "FontName=$targetFamilyName")
                MPVLib.setOptionString("sub-ass-force-style", "FontName=$targetFamilyName")
            } else {
                MPVLib.setPropertyString("embeddedfonts", "yes")
                MPVLib.setOptionString("embeddedfonts", "yes")
                MPVLib.setPropertyString("sub-font", "")
                MPVLib.setOptionString("sub-font", "")
                MPVLib.setPropertyString("osd-font", "")
                MPVLib.setOptionString("osd-font", "")
                MPVLib.setPropertyString("sub-ass-force-style", "")
                MPVLib.setOptionString("sub-ass-force-style", "")
            }

            // إعادة تحميل الترجمة وتحديث العرض فوراً
            try {
                MPVLib.command("sub-reload")
            } catch (_: Throwable) {}

            try {
                val currentSid = MPVLib.getPropertyString("sid")
                if (!currentSid.isNullOrBlank() && currentSid != "no" && currentSid != "auto") {
                    MPVLib.setPropertyString("sid", currentSid)
                }
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
