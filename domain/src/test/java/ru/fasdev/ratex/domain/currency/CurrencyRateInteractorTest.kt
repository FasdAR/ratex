package ru.fasdev.ratex.domain.currency

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currency.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

public final class CurrencyRateInteractorTest
{
    private lateinit var currencyRateRepo: CurrencyRateRepo

    private lateinit var currencyRateInteractor: CurrencyRateInteractor

    @Before
    fun setUp() {
        currencyRateRepo = Mockito.mock(CurrencyRateRepo::class.java)

        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo)
    }

    @Test
    fun getExchangeRates()
    {
        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        val testData: MutableList<RateCurrencyDomain> = arrayListOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 0.534786),
            RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 0.563423)
        )

        Mockito.`when`(currencyRateRepo.getExchangeRates()).thenReturn(Single.just(testData))

        currencyRateInteractor.getExchangeRates().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue(testData.sortedBy { it -> it.currency.displayName })
    }
}