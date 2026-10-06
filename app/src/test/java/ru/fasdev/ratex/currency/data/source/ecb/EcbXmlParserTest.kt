package ru.fasdev.ratex.currency.data.source.ecb

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EcbXmlParserTest {
    @Test
    fun testParseDaily() {
        val result = EcbXmlParser.parse(EcbTestData.XML_DAILY)

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.date).isEqualTo("2026-10-05")
        assertThat(result.rates).containsExactlyEntriesOf(
            linkedMapOf("USD" to 1.0850, "JPY" to 162.40, "GBP" to 0.8412, "PLN" to 4.2791)
        )
    }

    @Test
    fun testParseSkipsNonPositiveAndNonNumericRates() {
        val result = EcbXmlParser.parse(EcbTestData.XML_WITH_BAD_RATES)

        assertThat(result.rates).containsOnlyKeys("USD")
    }

    @Test
    fun testParseWithoutRatesThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.XML_NO_RATES) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseHtmlErrorPageThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.HTML_ERROR_PAGE) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun testParseMalformedXmlThrows() {
        val error = runCatching { EcbXmlParser.parse(EcbTestData.NOT_XML) }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
