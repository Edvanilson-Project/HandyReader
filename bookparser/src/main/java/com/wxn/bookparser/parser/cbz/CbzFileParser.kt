package com.wxn.bookparser.parser.cbz

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.extension
import com.wxn.base.bean.Book
import com.wxn.bookparser.FileParser
import com.wxn.bookparser.domain.file.CachedFile
import com.wxn.bookparser.exts.rawFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

class CbzFileParser @Inject constructor(val context: Context) : FileParser {

    override suspend fun parse(file: DocumentFile): Book? {
        val rawFile = file.rawFile(context)
        val title = file.baseName
        val path = file.uri.toString()
        val format = file.extension
        return innerParse(rawFile, title, path, format)
    }

    override suspend fun parse(cachedFile: CachedFile): Book? {
        val rawFile = cachedFile.rawFile
        val title = cachedFile.name.substringBeforeLast(".").trim()
        val path = cachedFile.uri.toString()
        val format = cachedFile.extension
        return innerParse(rawFile, title, path, format)
    }

    private fun innerParse(rawFile: File?, title: String, uriPath: String, format: String): Book? {
        if (rawFile == null || !rawFile.isFile || !rawFile.exists() || !rawFile.canRead()) {
            return null
        }

        var coverPath = ""
        try {
            ZipFile(rawFile).use { zip ->
                val imageEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && isImageFile(it.name) }
                    .sortedWith { e1, e2 -> compareFileNames(e1.name, e2.name) }
                    .toList()

                if (imageEntries.isEmpty()) {
                    return null
                }

                val firstEntry = imageEntries.first()
                val coverDir = File(context.cacheDir, "cbz_covers").apply { mkdirs() }
                val targetCoverFile = File(coverDir, "${rawFile.nameWithoutExtension}_cover.${firstEntry.name.substringAfterLast('.')}")

                if (!targetCoverFile.exists() || targetCoverFile.length() == 0L) {
                    zip.getInputStream(firstEntry).use { input ->
                        FileOutputStream(targetCoverFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                coverPath = targetCoverFile.absolutePath
            }
        } catch (e: Exception) {
            return null
        }

        return Book(
            title = title,
            author = "",
            publisher = "",
            description = "Comic Book Archive",
            language = "",
            review = "",
            scrollIndex = 0,
            scrollOffset = 0,
            progress = 0f,
            filePath = uriPath,
            lastOpened = null,
            category = "Comic",
            coverImage = coverPath,
            fileType = format
        )
    }

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

        fun isImageFile(name: String): Boolean {
            if (name.startsWith("__MACOSX") || name.startsWith(".")) return false
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in IMAGE_EXTENSIONS
        }

        fun compareFileNames(name1: String, name2: String): Int {
            return name1.compareTo(name2, ignoreCase = true)
        }
    }
}
