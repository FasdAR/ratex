package ru.fasdev.ratex.currency.data.repo

import java.util.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class CurrencyBaseRepoTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock lateinit var sharedPrefencesRepo: SharedPrefencesRepo

    @Mock lateinit var currencyImageRepo: CurrencyImageRepo

    lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Before
    fun setUp() {
        currencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo, UnconfinedTestDispatcher())
    }

    @Test
    fun testGetBaseCurrencyNullPreferences() = runTest {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)

        val testLocale = Locale.US
        Locale.setDefault(testLocale)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo(Currency.getInstance(Locale.getDefault()).currencyCode)
    }

    @Test
    fun testGetBaseCurrency() = runTest {
        val testLocale = Locale.US
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(Currency.getInstance(testLocale).currencyCode)

        val result = currencyBaseRepo.getBaseCurrency()

        assertThat(result.currencyCode).isEqualTo(Currency.getInstance(testLocale).currencyCode)
    }

    @Test
    fun testSetBaseCurrency() {
        val testCurrencyCode = "RUB"
        currencyBaseRepo.setBaseCurrency(CurrencyDomain.getInstance(testCurrencyCode))

        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode(testCurrencyCode)
    }

    @Test
    fun testGetAvailableCurrencies() = runTest {
        val result = currencyBaseRepo.getAvailableCurrencies()

        assertThat(result).isNotEmpty()
    }
}
