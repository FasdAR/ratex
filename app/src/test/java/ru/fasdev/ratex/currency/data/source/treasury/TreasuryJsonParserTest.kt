package ru.fasdev.ratex.currency.data.source.treasury

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test

class TreasuryJsonParserTest {
    @Test
    fun testParseUsesRatesPerUsdAsIs() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_DAILY)

        assertThat(result.baseCode).isEqualTo("USD")
        assertThat(result.date).isEqualTo("2026-09-30")
        assertThat(result.rates.getValue("EUR")).isCloseTo(0.881, Offset.offset(1e-9))
        assertThat(result.rates.getValue("JPY")).isCloseTo(157.09, Offset.offset(1e-9))
        assertThat(result.rates.getValue("ZWG")).isCloseTo(25.337, Offset.offset(1e-9))
    }

    @Test
    fun testParseIgnoresRowsFromEarlierDates() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_DAILY)

        assertThat(result.rates).containsOnlyKeys("EUR", "JPY", "AUD", "ZWG")
    }

    @Test
    fun testParseCollapsesCountriesWithSameCurrency() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_SHARED_CURRENCY)

        assertThat(result.rates).containsOnlyKeys("XOF", "XAF")
        assertThat(result.rates.getValue("XOF")).isCloseTo(573.75, Offset.offset(1e-9))
        assertThat(result.rates.getValue("XAF")).isCloseTo(577.72, Offset.offset(1e-9))
    }

    @Test
    fun testParseSkipsUsdPeggedEntries() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_USD_PEGGED)

        assertThat(result.rates).containsOnlyKeys("JPY")
    }

    @Test
    fun testParseSkipsKnownBadRows() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_KNOWN_BAD_ROWS)

        assertThat(result.rates).containsOnlyKeys("JPY")
    }

    @Test
    fun testParseSkipsInvalidValuesAndUnknownDescriptions() {
        val result = TreasuryJsonParser.parse(TreasuryTestData.JSON_WITH_BAD_VALUES)

        assertThat(result.rates).containsOnlyKeys("JPY")
    }

    @Test
    fun testParseWithoutRowsThrows() {
        val error = runCatching { TreasuryJsonParser.parse(TreasuryTestData.JSON_NO_ROWS) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseOnlyUnknownDescriptionsThrows() {
        val error = runCatching { TreasuryJsonParser.parse(TreasuryTestData.JSON_ONLY_UNKNOWN) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseWithoutDataFieldThrows() {
        val error = runCatching { TreasuryJsonParser.parse(TreasuryTestData.JSON_NO_DATA_FIELD) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { TreasuryJsonParser.parse(TreasuryTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }

    @Test
    fun testCurrencyTableHoldsOnlyForeignIsoCodes() {
        val codes = TreasuryCurrencyCodes.byDescription.values

        assertThat(codes).allMatch { it.matches(Regex("[A-Z]{3}")) }
        assertThat(codes).doesNotContain("USD")
        assertThat(TreasuryCurrencyCodes.byDescription).doesNotContainKeys("Nepal-Rupee", "New Zealand-Dollar", "Curacao-Caribbean Guilder")
    }
}
