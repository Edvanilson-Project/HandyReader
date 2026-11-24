package com.wxn.reader.util.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Utilitário de pronúncia rápida para palavras ou frases selecionadas pelo usuário no leitor.
 * Permite reprodução imediata com o motor TTS do sistema sem interferir no estado do serviço
 * de leitura de capítulos.
 */
object QuickPronouncer {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun speak(context: Context, text: String, locale: Locale = Locale.US) {
        if (text.isBlank()) return
        val cleanText = text.trim()
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    tts?.language = locale
                    tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "quick_pronounce")
                }
            }
        } else {
            tts?.language = locale
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "quick_pronounce")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
