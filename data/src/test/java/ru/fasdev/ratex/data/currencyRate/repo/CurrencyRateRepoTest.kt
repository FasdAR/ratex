package ru.fasdev.ratex.data.currencyRate.repo

import io.reactivex.Single
import io.reactivex.observers.TestObserver
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
    fun testGetExchangeRates() {
        val testCurrencyDomain = CurrencyDomain.getInstance("RUB")

        val testListData = listOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 73.0),
            RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 90.5)
        )

        Mockito.`when`(currencyBaseRepo.getBaseCurrency())
            .thenReturn(Single.just(testCurrencyDomain))

        Mockito
            .`when`(currencyRateDataStore.getExchangeRates(testCurrencyDomain))
            .thenReturn(
                Single.just(
                    testListData
                )
            )

        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        currencyRateRepo.getExchangeRates().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue(testListData)
    }
}
