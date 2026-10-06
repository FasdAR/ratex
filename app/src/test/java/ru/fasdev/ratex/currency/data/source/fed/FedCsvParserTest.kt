package ru.fasdev.ratex.currency.data.source.fed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test

class FedCsvParserTest {
    @Test
    fun testParseConvertsBothQuoteDirections() {
        val result = FedCsvParser.parse(FedTestData.CSV_DAILY)

        assertThat(result.baseCode).isEqualTo("USD")
        assertThat(result.date).isEqualTo("2026-10-02")
        assertThat(result.rates).containsOnlyKeys("AUD", "EUR", "JPY", "VES")
        // RXI_*: единиц валюты за 1 USD
        assertThat(result.rates.getValue("JPY")).isCloseTo(157.81, Offset.offset(1e-9))
        // RXI$US_*: USD за 1 единицу, в снимке нужна обратная величина
        assertThat(result.rates.getValue("EUR")).isCloseTo(1 / 1.1259, Offset.offset(1e-9))
        assertThat(result.rates.getValue("AUD")).isCloseTo(1 / 0.6953, Offset.offset(1e-9))
    }

    @Test
    fun testParseMapsVenezuelanBolivarCode() {
        val result = FedCsvParser.parse(FedTestData.CSV_DAILY)

        assertThat(result.rates).doesNotContainKey("VEB")
        assertThat(result.rates.getValue("VES")).isCloseTo(864.3948, Offset.offset(1e-9))
    }

    @Test
    fun testParseSkipsHolidayRowAndUsesPreviousDate() {
        val result = FedCsvParser.parse(FedTestData.CSV_LAST_ROW_ND)

        assertThat(result.date).isEqualTo("2026-10-01")
        assertThat(result.rates.getValue("JPY")).isCloseTo(157.63, Offset.offset(1e-9))
    }

    @Test
    fun testParseTakesLastNumericValueForEachCurrency() {
        val result = FedCsvParser.parse(FedTestData.CSV_PARTIAL_ND)

        assertThat(result.date).isEqualTo("2026-10-02")
        assertThat(result.rates.getValue("JPY")).isCloseTo(157.63, Offset.offset(1e-9))
        assertThat(result.rates.getValue("EUR")).isCloseTo(1 / 1.1259, Offset.offset(1e-9))
    }

    @Test
    fun testParseSkipsInvalidValues() {
        val result = FedCsvParser.parse(FedTestData.CSV_WITH_BAD_VALUES)

        assertThat(result.rates).containsOnlyKeys("JPY")
    }

    @Test
    fun testParseToleratesSpaceInRowLabel() {
        val result = FedCsvParser.parse(FedTestData.CSV_LABEL_WITH_SPACE)

        assertThat(result.rates.getValue("EUR")).isCloseTo(1 / 1.1259, Offset.offset(1e-9))
        assertThat(result.rates.getValue("JPY")).isCloseTo(157.81, Offset.offset(1e-9))
    }

    @Test
    fun testParseOnlyNdThrows() {
        val error = runCatching { FedCsvParser.parse(FedTestData.CSV_ONLY_ND) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseWithoutCurrencyRowThrows() {
        val error = runCatching { FedCsvParser.parse(FedTestData.CSV_NO_CURRENCY_ROW) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { FedCsvParser.parse(FedTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}
