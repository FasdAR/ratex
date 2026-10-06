package ru.fasdev.ratex.currency.domain.entity

import java.util.Currency
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.fasdev.ratex.currency.domain.entity.extension.toCurrencyDomain

class CurrencyDomainTest {
    @Test
    fun testInstance() {
        val testCurrencyCode = "USD"

        val result = CurrencyDomain.getInstance(testCurrencyCode)

        assertThat(result)
            .isNotNull()
            .extracting { it.currencyCode }
            .isEqualTo(testCurrencyCode)
    }

    @Test
    fun testConvertToCurrencyDomain() {
        val testCurrency = Currency.getInstance("USD")
        val currencyDomain = testCurrency.toCurrencyDomain()

        assertThat(currencyDomain).isNotNull()

        assertThat(currencyDomain.currencyCode).isEqualTo(testCurrency.currencyCode)
        assertThat(currencyDomain.symbol).isEqualTo(testCurrency.symbol)
        assertThat(currencyDomain.displayName).isEqualTo(testCurrency.displayName.replaceFirstChar { it.uppercase() })
    }

    @Test
    fun testEqualsTrue() {
        val testData = CurrencyDomain("USD", "$", "DOLLAR", null)
        val testDataTwo = CurrencyDomain("USD", "$", "DOLLAR", null)

        assertThat(testData.equals(testDataTwo))
            .isTrue()
    }

    @Test
    fun testEqualsFalse() {
        val testData = CurrencyDomain("USD", "$", "DOLLAR", null)
        val testDataTwo = CurrencyDomain("SDD", "$", "DOLLAR", "httptpt")

        assertThat(testData.equals(testDataTwo))
            .isFalse()
    }
}
