package com.wxn.reader.util.export

import com.wxn.reader.data.dto.BookVocabularyEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário de exportação de vocabulário e anotações.
 * Suporta Anki (CSV tab-separated para flashcards de estudo de idiomas)
 * e Markdown estruturado (Obsidian, Notion, Logseq).
 */
object VocabularyExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Exporta entradas de vocabulário para CSV/TSV perfeitamente compatível com o Anki.
     * Coluna 1: Frente (Palavra)
     * Coluna 2: Contexto da Frase (com a palavra destacada em <b>negrito</b>)
     * Coluna 3: Idioma
     * Coluna 4: Data de Adição
     */
    fun exportToAnkiCsv(entries: List<BookVocabularyEntity>): String {
        val sb = StringBuilder()
        // Cabeçalho de metadados para o Anki
        sb.append("#separator:tab\n")
        sb.append("#html:true\n")
        sb.append("#tags column:3\n")

        for (entry in entries) {
            val word = entry.word.trim()
            val formattedDate = dateFormat.format(Date(entry.createdAt))
            // Destaca a palavra na frase em negrito para facilitar o aprendizado
            val highlightedSentence = highlightWordInSentence(entry.sentenceText, word)

            // Escapa tabulações e quebras de linha
            val cleanWord = escapeField(word)
            val cleanSentence = escapeField(highlightedSentence)
            val langTag = "lang_${entry.lang.lowercase()}"

            sb.append(cleanWord).append("\t")
            sb.append(cleanSentence).append("\t")
            sb.append(langTag).append("\t")
            sb.append(formattedDate).append("\n")
        }

        return sb.toString()
    }

    /**
     * Exporta entradas de vocabulário para Markdown estruturado (Obsidian / Notion).
     */
    fun exportToMarkdown(
        entries: List<BookVocabularyEntity>,
        bookTitle: String = "Vocabulário HandyReader"
    ): String {
        val sb = StringBuilder()
        sb.append("# 📚 ").append(bookTitle).append("\n\n")
        sb.append("> *Exportado pelo HandyReader em ").append(dateFormat.format(Date())).append("*\n")
        sb.append("> *Total de termos aprendidos: ").append(entries.size).append("*\n\n")
        sb.append("---\n\n")

        for ((index, entry) in entries.withIndex()) {
            val word = entry.word.trim()
            val formattedDate = dateFormat.format(Date(entry.createdAt))
            val highlightedSentence = highlightWordInSentence(entry.sentenceText, word)

            sb.append("### ").append(index + 1).append(". **").append(word).append("** (`").append(entry.lang).append("`)\n\n")
            if (highlightedSentence.isNotBlank()) {
                sb.append("> \"").append(highlightedSentence).append("\"\n\n")
            }
            sb.append("- ⏱ *Adicionado em:* `").append(formattedDate).append("`\n\n")
        }

        return sb.toString()
    }

    private fun highlightWordInSentence(sentence: String, word: String): String {
        if (sentence.isBlank() || word.isBlank()) return sentence
        val regex = Regex("(?i)\\b(${Regex.escape(word)})\\b")
        return if (regex.containsMatchIn(sentence)) {
            sentence.replace(regex, "<b>$1</b>")
        } else {
            sentence
        }
    }

    private fun escapeField(text: String): String {
        return text.replace("\t", " ")
            .replace("\r\n", "<br>")
            .replace("\n", "<br>")
            .trim()
    }
}
