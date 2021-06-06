package com.szareckii.dictionary

import com.szareckii.dictionary.utils.SearchResultParser
import com.szareckii.model.data.userdata.Meaning
import com.szareckii.model.data.userdata.TranslatedMeaning
import org.junit.Assert.*
import org.junit.Test

class DictionaryUnitTest {

    private val mapper = SearchResultParser()

    @Test
    fun convertMeaningsToString_NotNull() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )
        val outputData = mapper.convertMeaningsToString(inputData)

        assertNotNull(outputData)
    }

    @Test
    fun convertMeaningsToString_isCorrect() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )

        val meaningsSeparatedByComma = "translatedMeaning1, translatedMeaning2"
        val outputData = mapper.convertMeaningsToString(inputData)

        assertEquals(outputData, meaningsSeparatedByComma)
    }

    @Test
    fun convertMeaningsToString_isNotCorrect_withoutComa() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )

        val meaningsSeparatedByComma = "translatedMeaning1 translatedMeaning2"
        val outputData = mapper.convertMeaningsToString(inputData)

        assertNotEquals(outputData, meaningsSeparatedByComma)
    }

    @Test
    fun convertMeaningsToString_isNotCorrect1_withoutSpace() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )

        val meaningsSeparatedByComma = "translatedMeaning1,translatedMeaning2"
        val outputData = mapper.convertMeaningsToString(inputData)

        assertNotEquals(outputData, meaningsSeparatedByComma)
    }

    @Test
    fun convertMeaningsToString_isNotCorrectwithDoubleSpace() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )

        val meaningsSeparatedByComma = "translatedMeaning1,  translatedMeaning2"
        val outputData = mapper.convertMeaningsToString(inputData)

        assertNotEquals(outputData, meaningsSeparatedByComma)
    }

    private val arrayOne: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    private val arrayTwoNull: Array<Int>? = null

    @Test
    fun demoForTestArrayEquals_assertArrayEquals() {
        val outputData = mapper.demoForTestArrayEquals(arrayOne)
        assertArrayEquals(outputData, arrayOne)
    }

    @Test
    fun demoForTestArrayEquals_assertNull() {
        val outputData = mapper.demoForTestArrayEquals(arrayTwoNull)
        assertNull(outputData)
    }

    @Test
    fun convertMeaningsToString_isNotSame() {
        val translatedMeaning1 = TranslatedMeaning("translatedMeaning1")
        val translatedMeaning2 = TranslatedMeaning("translatedMeaning2")
        val inputData = listOf(
            Meaning(translatedMeaning1,"image url 1", "transcription 1"),
            Meaning(translatedMeaning2,"image url 2", "transcription 2")
        )

        val meaningsSeparatedByComma = "translatedMeaning1, translatedMeaning2"
        val outputData = mapper.convertMeaningsToString(inputData)

        assertNotSame(outputData, meaningsSeparatedByComma)
    }
}