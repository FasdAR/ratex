package ru.fasdev.ratex.domain.currency.entity

import junit.framework.Assert.*
import org.junit.Test
import ru.fasdev.ratex.domain.currency.entity.extension.toCurrencyDomain
import java.util.*

class CurrencyDomainTest {

    @Test
    fun getInstance()
    {
        val result = CurrencyDomain.getInstance("USD")

        assertNotNull(result)
        assertEquals(result.symbol, "$")
    }

    @Test
    fun toCurrencyDomain()
    {
        val currency = Currency.getInstance("USD")
        val currencyDomain = currency.toCurrencyDomain()

        assertNotNull(currencyDomain)

        assertEquals(currencyDomain.symbol, currency.symbol)
        assertEquals(currencyDomain.currencyCode, currency.currencyCode)
        assertEquals(currencyDomain.displayName, currency.displayName)
    }
}