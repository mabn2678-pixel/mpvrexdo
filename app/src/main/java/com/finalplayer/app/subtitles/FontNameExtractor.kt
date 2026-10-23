package com.finalplayer.app.subtitles

import java.io.File
import java.io.RandomAccessFile

/**
 * مستخرج اسم عائلة الخط (Font Family Name) من ترويسة ملفات TTF و OTF
 * لضمان مطابقة الاسم الداخلي الدقيق مع محرك Fontconfig و libass في MPV.
 */
object FontNameExtractor {
    fun extractFamilyName(fontFile: File): String {
        try {
            if (!fontFile.exists() || fontFile.length() < 12) {
                return formatFallbackName(fontFile.nameWithoutExtension)
            }

            RandomAccessFile(fontFile, "r").use { raf ->
                raf.seek(0)
                val sfntVersion = raf.readInt()
                val numTables = raf.readUnsignedShort()
                raf.skipBytes(6) // searchRange, entrySelector, rangeShift

                var nameTableOffset = -1L
                for (i in 0 until numTables) {
                    val tag = raf.readInt()
                    val checksum = raf.readInt()
                    val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                    val length = raf.readInt().toLong() and 0xFFFFFFFFL
                    if (tag == 0x6E616D65) { // 'name' tag in ASCII
                        nameTableOffset = offset
                        break
                    }
                }
                if (nameTableOffset == -1L) {
                    return formatFallbackName(fontFile.nameWithoutExtension)
                }

                raf.seek(nameTableOffset)
                val format = raf.readUnsignedShort()
                val count = raf.readUnsignedShort()
                val stringOffset = raf.readUnsignedShort()
                val stringBase = nameTableOffset + stringOffset

                val records = mutableListOf<NameRecord>()
                for (i in 0 until count) {
                    val platformId = raf.readUnsignedShort()
                    val encodingId = raf.readUnsignedShort()
                    val languageId = raf.readUnsignedShort()
                    val nameId = raf.readUnsignedShort()
                    val length = raf.readUnsignedShort()
                    val offset = raf.readUnsignedShort()
                    records.add(NameRecord(platformId, encodingId, languageId, nameId, length, offset))
                }

                // نفضل قراءة nameId == 1 (Font Family) أولاً
                for (rec in records.filter { it.nameId == 1 }) {
                    raf.seek(stringBase + rec.offset)
                    val bytes = ByteArray(rec.length)
                    raf.readFully(bytes)
                    val str = if (rec.platformId == 3 || rec.platformId == 0) {
                        String(bytes, Charsets.UTF_16BE)
                    } else {
                        String(bytes, Charsets.UTF_8)
                    }
                    val clean = str.trim().replace("\u0000", "")
                    if (clean.isNotBlank()) return clean
                }

                // أو nameId == 4 (Full Name) كبديل
                for (rec in records.filter { it.nameId == 4 }) {
                    raf.seek(stringBase + rec.offset)
                    val bytes = ByteArray(rec.length)
                    raf.readFully(bytes)
                    val str = if (rec.platformId == 3 || rec.platformId == 0) {
                        String(bytes, Charsets.UTF_16BE)
                    } else {
                        String(bytes, Charsets.UTF_8)
                    }
                    val clean = str.trim().replace("\u0000", "")
                    if (clean.isNotBlank()) return clean
                }
            }
        } catch (_: Throwable) {}

        return formatFallbackName(fontFile.nameWithoutExtension)
    }

    private fun formatFallbackName(raw: String): String {
        return raw.replace('_', ' ').replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private data class NameRecord(
        val platformId: Int,
        val encodingId: Int,
        val languageId: Int,
        val nameId: Int,
        val length: Int,
        val offset: Int
    )
}
