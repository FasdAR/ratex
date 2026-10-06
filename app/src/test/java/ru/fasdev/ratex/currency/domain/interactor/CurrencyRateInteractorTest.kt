package ru.fasdev.ratex.currency.domain.interactor

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyRateInteractorTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock private lateinit var currencyRateRepo: CurrencyRateRepo

    @Mock private lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Mock private lateinit var currencyImageRepo: CurrencyImageRepo

    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    private val snapshot = RateSnapshotDomain(
        "EUR",
        "2026-10-05",
        mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5, "ZZZ" to 9.0)
    )

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyRateRepo.getSnapshot()).thenReturn(snapshot)
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, currencyBaseRepo, currencyImageRepo)
    }

    private suspend fun ratesFor(baseCode: String): Map<String, Double> {
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(CurrencyDomain.getInstance(baseCode))
        return currencyRateInteractor.getExchangeRates().associate { it.currency.currencyCode to it.rate }
    }

    @Test
    fun testGetExchangeRatesForSourceBase() = runTest {
        assertThat(ratesFor("EUR")).isEqualTo(mapOf("USD" to 2.0, "JPY" to 200.0, "GBP" to 0.5))
    }

    @Test
    fun testGetExchangeRatesForOtherBaseIsCrossRate() = runTest {
        assertThat(ratesFor("USD")).isEqualTo(mapOf("EUR" to 0.5, "JPY" to 100.0, "GBP" to 0.25))
    }

    @Test
    fun testGetExchangeRatesSortedByDisplayName() = runTest {
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(CurrencyDomain.getInstance("EUR"))

        val result = currencyRateInteractor.getExchangeRates()

        assertThat(result.map { it.currency.displayName }).isEqualTo(result.map { it.currency.displayName }.sorted())
    }

    @Test
    fun testGetExchangeRatesBaseMissingInSnapshotIsEmpty() = runTest {
        assertThat(ratesFor("RUB")).isEmpty()
    }
}
