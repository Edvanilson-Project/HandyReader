package com.wxn.reader.data.remote.api

import com.wxn.reader.data.model.DictDefinition
import com.wxn.reader.data.model.PhoneticItem
import com.wxn.reader.data.model.WordResult
import com.wxn.reader.data.remote.dto.BaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONArray
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryApiImpl @Inject constructor(
    private val httpClient: HttpClient
) : DictionaryApi {

    override suspend fun lookup(word: String, lang: String): Result<BaseResponse<WordResult>> {
        val primaryResult = BaseApi.get(
            httpClient,
            "${Constants.DICTIONARY_BASE_URL}${ApiPath.API_DICTIONARY}",
            mapOf("word" to word, "lang" to lang),
            isFullUrl = true
        )
        if (primaryResult.isSuccess && primaryResult.getOrNull()?.data?.hasResult == true) {
            return primaryResult
        }

        // Fallback aberto de alta disponibilidade para inglês via Free Dictionary API oficial (CC BY-SA)
        if (lang.equals("en", ignoreCase = true)) {
            val fallback = lookupFreeDictionaryEn(word)
            if (fallback.isSuccess) {
                return fallback
            }
        }

        return primaryResult
    }

    private suspend fun lookupFreeDictionaryEn(word: String): Result<BaseResponse<WordResult>> {
        return try {
            val encodedWord = URLEncoder.encode(word.trim(), "UTF-8")
            val response = httpClient.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encodedWord")
            if (response.status.value in 200..299) {
                val jsonString = response.bodyAsText()
                val parsed = parseFreeDictionaryJson(word, jsonString)
                if (parsed != null && parsed.hasResult) {
                    Result.success(BaseResponse(code = 200, message = "OK", success = true, data = parsed))
                } else {
                    Result.failure(Exception("FreeDictionary: No definitions found"))
                }
            } else {
                Result.failure(Exception("FreeDictionary HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFreeDictionaryJson(word: String, jsonString: String): WordResult? {
        return try {
            val array = JSONArray(jsonString)
            if (array.length() == 0) return null
            val first = array.getJSONObject(0)
            val phonetic = first.optString("phonetic", null)
            val phoneticsList = mutableListOf<PhoneticItem>()
            val phoneticsArray = first.optJSONArray("phonetics")
            if (phoneticsArray != null) {
                for (i in 0 until phoneticsArray.length()) {
                    val pObj = phoneticsArray.getJSONObject(i)
                    val text = pObj.optString("text", null)
                    val audio = pObj.optString("audio", null)
                    if (!audio.isNullOrBlank() || !text.isNullOrBlank()) {
                        phoneticsList.add(PhoneticItem(text = text, audio = audio))
                    }
                }
            }

            val definitionsList = mutableListOf<DictDefinition>()
            val meaningsArray = first.optJSONArray("meanings")
            if (meaningsArray != null) {
                for (i in 0 until meaningsArray.length()) {
                    val mObj = meaningsArray.getJSONObject(i)
                    val pos = mObj.optString("partOfSpeech", "")
                    val defs = mObj.optJSONArray("definitions")
                    if (defs != null) {
                        for (j in 0 until defs.length()) {
                            val dObj = defs.getJSONObject(j)
                            val defText = dObj.optString("definition", "")
                            val example = dObj.optString("example", null)
                            val syns = mutableListOf<String>()
                            val synArray = dObj.optJSONArray("synonyms")
                            if (synArray != null) {
                                for (k in 0 until synArray.length()) {
                                    syns.add(synArray.getString(k))
                                }
                            }
                            val ants = mutableListOf<String>()
                            val antArray = dObj.optJSONArray("antonyms")
                            if (antArray != null) {
                                for (k in 0 until antArray.length()) {
                                    ants.add(antArray.getString(k))
                                }
                            }
                            if (defText.isNotBlank()) {
                                definitionsList.add(
                                    DictDefinition(
                                        partOfSpeech = pos,
                                        definition = defText,
                                        example = example,
                                        synonyms = syns,
                                        antonyms = ants,
                                        note = "FreeDictionary"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            WordResult(
                word = first.optString("word", word),
                lang = "en",
                source = "FreeDictionary",
                phonetic = phonetic,
                phonetics = phoneticsList,
                definitions = definitionsList
            )
        } catch (_: Exception) {
            null
        }
    }
}

