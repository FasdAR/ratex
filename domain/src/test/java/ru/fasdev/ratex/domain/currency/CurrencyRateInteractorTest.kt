package ru.fasdev.ratex.domain.currency

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currency.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

class CurrencyRateInteractorTest
{
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock private lateinit var currencyRateRepo: CurrencyRateRepo

    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    @Before
    fun setUp() {
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo)
    }

    @Test
    fun testGetExchangeRates()
    {
        val testData: MutableList<RateCurrencyDomain> = arrayListOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 0.534786),
            RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 0.563423)
        )

        Mockito.`when`(currencyRateRepo.getExchangeRates()).thenReturn(Single.just(testData))

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        currencyRateInteractor.getExchangeRates().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue(testData.sortedBy { it.currency.displayName })
    }
}