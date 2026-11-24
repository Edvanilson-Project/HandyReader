package com.wxn.bookparser.parser.cbz

import android.content.Context
import android.util.Log
import com.wxn.base.bean.BookChapter
import com.wxn.base.bean.ReaderText
import com.wxn.bookparser.TextParser
import com.wxn.bookparser.domain.file.CachedFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

class CbzTextParser @Inject constructor(private val context: Context) : TextParser {

    override suspend fun parseChapterInfo(
        bookId: Long,
        cachedFile: CachedFile
    ): List<BookChapter> {
        val rawFile = cachedFile.rawFile
        if (rawFile == null || !rawFile.isFile || !rawFile.canRead()) {
            Log.e(TAG, "parseChapterInfo failed, file unreadable: $cachedFile")
            return emptyList()
        }

        return try {
            ZipFile(rawFile).use { zip ->
                val imageEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && CbzFileParser.isImageFile(it.name) }
                    .sortedWith { e1, e2 -> CbzFileParser.compareFileNames(e1.name, e2.name) }
                    .toList()

                val total = imageEntries.size
                imageEntries.mapIndexed { index, entry ->
                    BookChapter(
                        bookId = bookId,
                        chapterIndex = index,
                        chapterName = "Page ${index + 1}",
                        srcName = entry.name,
                        chaptersSize = total,
                        picCount = 1,
                        count = 1
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse CBZ entries", e)
            emptyList()
        }
    }

    override suspend fun parsedChapterData(
        bookId: Long,
        cachedFile: CachedFile,
        chapter: BookChapter
    ): List<ReaderText> {
        val rawFile = cachedFile.rawFile
        val entryName = chapter.srcName
        if (rawFile == null || entryName.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val pagesDir = File(context.cacheDir, "cbz_pages/$bookId").apply { mkdirs() }
            val safeFileName = "p_${chapter.chapterIndex}_" + entryName.substringAfterLast("/")
            val targetFile = File(pagesDir, safeFileName)

            if (!targetFile.exists() || targetFile.length() == 0L) {
                ZipFile(rawFile).use { zip ->
                    val entry = zip.getEntry(entryName) ?: return emptyList()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            listOf(ReaderText.Image(path = targetFile.absolutePath, width = 0, height = 0))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract CBZ page: $entryName", e)
            emptyList()
        }
    }

    override suspend fun getWordCount(
        bookId: Long,
        cachedFile: CachedFile
    ): List<Triple<Int, Int, Int>> {
        val chapters = parseChapterInfo(bookId, cachedFile)
        return chapters.map { Triple(it.chapterIndex, 0, 1) }
    }

    override suspend fun close(bookId: Long, cachedFile: CachedFile) {
        // Optional cleanup for the book's extracted pages cache
    }

    companion object {
        private const val TAG = "CbzTextParser"
    }
}
