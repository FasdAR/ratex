package ru.fasdev.ratex.currency.data.source.frankfurter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FrankfurterJsonParserTest {
    @Test
    fun testParse() {
        val snapshot = FrankfurterJsonParser.parse(FrankfurterTestData.JSON_RATES)

        assertThat(snapshot.baseCode).isEqualTo("EUR")
        assertThat(snapshot.rates).containsOnlyKeys("ARS", "GBP", "JPY", "PLN", "USD")
        assertThat(snapshot.rates["USD"]).isEqualTo(1.1239)
        assertThat(snapshot.rates["JPY"]).isEqualTo(177.69)
    }

    @Test
    fun testDateIsLatestOfQuotes() {
        val snapshot = FrankfurterJsonParser.parse(FrankfurterTestData.JSON_RATES)

        assertThat(snapshot.date).isEqualTo("2026-10-07")
    }

    @Test
    fun testBadRatesAndBaseAreSkipped() {
        val snapshot = FrankfurterJsonParser.parse(FrankfurterTestData.JSON_WITH_BAD_RATES)

        assertThat(snapshot.rates).containsOnlyKeys("USD")
    }

    @Test
    fun testOtherBaseThrows() {
        val error = runCatching { FrankfurterJsonParser.parse(FrankfurterTestData.JSON_OTHER_BASE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testEmptyThrows() {
        val error = runCatching { FrankfurterJsonParser.parse(FrankfurterTestData.JSON_EMPTY) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testNotJsonThrows() {
        val error = runCatching { FrankfurterJsonParser.parse(FrankfurterTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
