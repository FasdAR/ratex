package ru.fasdev.ratex.data.currencyRate.repo

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.data.currencyRate.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class CurrencyRateRepoTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock private lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Mock private lateinit var currencyRateDataStore: CurrencyRateDataStore

    private lateinit var currencyRateRepo: CurrencyRateRepo

    @Before fun setUp() {
        currencyRateRepo = CurrencyRateRepoImpl(currencyRateDataStore, currencyBaseRepo)
    }

    @Test
    fun testGetExchangeRates() = runTest {
        val testCurrencyDomain = CurrencyDomain.getInstance("RUB")

        val testListData = listOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 73.0),
            RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 90.5)
        )

        Mockito.`when`(currencyBaseRepo.getBaseCurrency())
            .thenReturn(testCurrencyDomain)

        Mockito
            .`when`(currencyRateDataStore.getExchangeRates(testCurrencyDomain))
            .thenReturn(testListData)

        val result = currencyRateRepo.getExchangeRates()

        assertThat(result).isEqualTo(testListData)
    }
}
