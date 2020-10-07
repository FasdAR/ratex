package ru.fasdev.ratex.domain.currency.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.fasdev.ratex.domain.currency.entity.extension.toCurrencyDomain
import java.util.*

class CurrencyDomainExtensionTest
{
    @Test
    fun testConvertToCurrencyDomain() {
        val currency = Currency.getInstance("USD")
        val convert = currency.toCurrencyDomain()

        assertThat(convert.currencyCode).isEqualTo(currency.currencyCode)
        assertThat(convert.displayName).isEqualTo(currency.displayName)
        assertThat(convert.symbol).isEqualTo(currency.symbol)
    }
}