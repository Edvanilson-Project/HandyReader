package com.wxn.reader.presentation.lookuphistory

import com.wxn.reader.data.model.WordResult

enum class SortBy {
    TIME_DESC, TIME_ASC, WORD_ASC, WORD_DESC
}

data class LookupHistoryUiState(
    val cards: List<LookupHistoryCard> = emptyList(),
    val availableBooks: Map<Long, String> = emptyMap(),
    val availableLangs: List<Pair<String, String>> = emptyList(),
    val selectedBookId: Long? = null,
    val selectedLang: Pair<String, String>? = null,
    val sortBy: SortBy = SortBy.TIME_DESC,
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalCount: Int = 0
)

data class LookupHistoryCard(
    val word: String,
    val lang: String,
    val wordResult: WordResult?,
    val positions: List<LookupHistoryPosition>,
    val isLoadingDefinition: Boolean = false,  //当前是否正在加载词汇释义
    val fetchFailed: Boolean = false,           //当前是否加载词汇释义失败
)

data class LookupHistoryPosition(
    val entryId: Long,
    val bookId: Long,
    val bookTitle: String,
    val sentenceText: String,
    val createdAt: Long
)

/**
 * Converte os cartões exibidos (já filtrados/ordenados) em entradas de vocabulário
 * para o [com.wxn.reader.util.export.VocabularyExporter]: uma linha por ocorrência (frase).
 * Cartões sem posição registrada geram uma linha sem frase.
 */
fun LookupHistoryUiState.toVocabularyEntries(): List<com.wxn.reader.data.dto.BookVocabularyEntity> =
    cards.flatMap { card ->
        if (card.positions.isEmpty()) {
            listOf(
                com.wxn.reader.data.dto.BookVocabularyEntity(
                    bookId = 0L,
                    lang = card.lang,
                    word = card.word,
                    sentenceText = "",
                    locator = "",
                )
            )
        } else {
            card.positions.map { pos ->
                com.wxn.reader.data.dto.BookVocabularyEntity(
                    bookId = pos.bookId,
                    lang = card.lang,
                    word = card.word,
                    sentenceText = pos.sentenceText,
                    locator = "",
                    createdAt = pos.createdAt,
                )
            }
        }
    }
