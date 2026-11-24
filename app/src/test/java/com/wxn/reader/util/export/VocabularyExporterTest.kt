package com.wxn.reader.util.export

import com.wxn.reader.data.dto.BookVocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de comportamento do [VocabularyExporter] (JVM puro, sem Android).
 */
class VocabularyExporterTest {

    private fun entry(
        word: String,
        sentence: String,
        lang: String = "en",
        createdAt: Long = 1_700_000_000_000L,
    ) = BookVocabularyEntity(
        bookId = 1L,
        lang = lang,
        word = word,
        sentenceText = sentence,
        locator = "{}",
        createdAt = createdAt,
    )

    @Test
    fun `anki exporta cabecalho e quatro colunas separadas por tab`() {
        val out = VocabularyExporter.exportToAnkiCsv(listOf(entry("book", "I read a book today.")))
        val lines = out.trimEnd('\n').split('\n')

        assertEquals("#separator:tab", lines[0])
        assertEquals("#html:true", lines[1])
        assertEquals("#tags column:3", lines[2])

        val cols = lines[3].split('\t')
        assertEquals(4, cols.size)
        assertEquals("book", cols[0])
        assertEquals("I read a <b>book</b> today.", cols[1])
        assertEquals("lang_en", cols[2])
    }

    @Test
    fun `destaque ignora maiusculas e minusculas e so casa palavra inteira`() {
        val out = VocabularyExporter.exportToAnkiCsv(
            listOf(entry("cat", "Cat and catalog and CAT."))
        )
        val sentence = out.trimEnd('\n').split('\n')[3].split('\t')[1]
        assertEquals("<b>Cat</b> and catalog and <b>CAT</b>.", sentence)
    }

    @Test
    fun `tab e quebra de linha na frase nao quebram o formato`() {
        val out = VocabularyExporter.exportToAnkiCsv(
            listOf(entry("run", "I run\tfast\nand far."))
        )
        val dataLines = out.trimEnd('\n').split('\n').drop(3)
        assertEquals("uma entrada deve ocupar exatamente uma linha", 1, dataLines.size)
        val cols = dataLines[0].split('\t')
        assertEquals(4, cols.size)
        assertTrue(cols[1].contains("<br>"))
    }

    @Test
    fun `frase sem a palavra e mantida sem destaque`() {
        val out = VocabularyExporter.exportToAnkiCsv(listOf(entry("zebra", "Nothing here.")))
        val sentence = out.trimEnd('\n').split('\n')[3].split('\t')[1]
        assertEquals("Nothing here.", sentence)
        assertFalse(sentence.contains("<b>"))
    }

    @Test
    fun `lista vazia gera apenas o cabecalho`() {
        val out = VocabularyExporter.exportToAnkiCsv(emptyList())
        assertEquals(3, out.trimEnd('\n').split('\n').size)
    }

    @Test
    fun `markdown traz titulo total e uma secao por termo`() {
        val out = VocabularyExporter.exportToMarkdown(
            listOf(entry("book", "A good book."), entry("read", "I read daily.")),
            bookTitle = "Meu Livro",
        )
        assertTrue(out.startsWith("# 📚 Meu Livro"))
        assertTrue(out.contains("Total de termos aprendidos: 2"))
        assertTrue(out.contains("### 1. **book** (`en`)"))
        assertTrue(out.contains("### 2. **read** (`en`)"))
        assertTrue(out.contains("<b>book</b>"))
    }
}
