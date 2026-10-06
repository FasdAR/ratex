package ru.fasdev.ratex.currency.domain.entity.extension

import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.fasdev.ratex.currency.domain.entity.extension.toCurrencyDomain

class CurrencyDomainExtensionTest {
    @Test
    fun testConvertToCurrencyDomain() {
        val currency = Currency.getInstance("USD")
        val convert = currency.toCurrencyDomain()

        assertThat(convert.currencyCode).isEqualTo(currency.currencyCode)
        assertThat(convert.displayName).isEqualTo(currency.displayName)
        assertThat(convert.symbol).isEqualTo(currency.symbol)
    }

    @Test
    fun testIsKnownCurrencyCode() {
        assertThat(isKnownCurrencyCode("USD")).isTrue()
        assertThat(isKnownCurrencyCode("ZZZ")).isFalse()
        assertThat(isKnownCurrencyCode("")).isFalse()
    }
}
