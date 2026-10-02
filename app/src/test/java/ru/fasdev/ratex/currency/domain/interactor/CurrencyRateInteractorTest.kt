package ru.fasdev.ratex.currency.domain.interactor

import java.util.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain
import ru.fasdev.ratex.currency.domain.interactor.CurrencyRateInteractorImpl

class CurrencyRateInteractorTest {
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock private lateinit var currencyRateRepo: CurrencyRateRepo

    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    @Before
    fun setUp() {
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo)
    }

    @Test
    fun testGetExchangeRates() = runTest {
        val testData: MutableList<RateCurrencyDomain> = arrayListOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 0.534786),
            RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 0.563423)
        )

        Mockito.`when`(currencyRateRepo.getExchangeRates()).thenReturn(testData)

        val result = currencyRateInteractor.getExchangeRates()

        assertThat(result).isEqualTo(testData.sortedBy { it.currency.displayName })
    }
}
