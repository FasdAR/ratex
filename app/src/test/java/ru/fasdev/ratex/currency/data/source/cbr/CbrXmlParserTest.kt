package ru.fasdev.ratex.currency.data.source.cbr

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CbrXmlParserTest {
    @Test
    fun testParseDailyUsesNominalAndComma() {
        val result = CbrXmlParser.parse(CbrTestData.XML_DAILY)

        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.date).isEqualTo("2026-10-05")
        assertThat(result.rates).containsOnlyKeys("USD", "JPY", "KZT", "BGN")
        // 1 RUB = Nominal / Value единиц валюты
        assertThat(result.rates.getValue("USD")).isCloseTo(1 / 92.5, Offset.offset(1e-9))
        assertThat(result.rates.getValue("JPY")).isCloseTo(100 / 61.2, Offset.offset(1e-9))
        assertThat(result.rates.getValue("KZT")).isCloseTo(100 / 18.9, Offset.offset(1e-9))
        assertThat(result.rates.getValue("BGN")).isCloseTo(1 / 50.0, Offset.offset(1e-9))
    }

    @Test
    fun testParseSkipsInvalidEntries() {
        val result = CbrXmlParser.parse(CbrTestData.XML_WITH_BAD_VALUES)

        assertThat(result.rates).containsOnlyKeys("USD")
    }

    @Test
    fun testParseWithoutRatesThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.XML_NO_RATES) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseBadDateThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.XML_BAD_DATE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { CbrXmlParser.parse(CbrTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
