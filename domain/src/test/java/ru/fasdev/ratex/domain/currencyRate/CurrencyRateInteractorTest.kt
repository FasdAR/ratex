package ru.fasdev.ratex.domain.currencyRate

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subscribers.TestSubscriber
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

public final class CurrencyRateInteractorTest
{
    private lateinit var currencyRateRepo: CurrencyRateRepo;
    private lateinit var sharedPrefencesRepo: SharedPrefencesRepo
    private lateinit var currencyRateInteractor: CurrencyRateInteractor
    private lateinit var currencyImageProvider: CurrencyImageProvider

    private lateinit var testObserver: TestObserver<List<RateCurrencyDomain>>

    @Before
    fun setUp() {
        currencyRateRepo = Mockito.mock(CurrencyRateRepo::class.java)
        sharedPrefencesRepo = Mockito.mock(SharedPrefencesRepo::class.java)
        currencyImageProvider = Mockito.mock(CurrencyImageProvider::class.java)
        currencyRateInteractor = CurrencyRateInteractorImpl(currencyRateRepo, sharedPrefencesRepo, currencyImageProvider)

        testObserver = TestObserver()
    }

    @Test
    fun getBaseCurrencyNotNull()
    {
        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        val testedCurrencyCode = "RUB"

        //#region Mock getBaseCurrencyCode and getDefaultLocale
        val rubCurrency = Currency.getInstance(testedCurrencyCode).currencyCode
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(rubCurrency)
        Locale.setDefault(Locale.US)
        //#endregion


        currencyRateInteractor.getBaseCurrency().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it -> it.currencyCode == rubCurrency }
    }

    @Test
    fun getBaseCurrencyNull()
    {
        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        //#region Mock getBaseCurrencyCode and getDefaultLocale
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)
        Locale.setDefault(Locale.US)
        //#endregion

        currencyRateInteractor.getBaseCurrency().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it -> it.currencyCode != "RUB" }
        testObserver.assertValue { it -> it.currencyCode == "USD" }
    }

    @Test
    fun getExchangeRates()
    {
        val testObserver: TestObserver<List<RateCurrencyDomain>> = TestObserver()

        //#region Mock getBaseCurrencyCode
        val testCurrency = Currency.getInstance("EUR").currencyCode
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(testCurrency)
        //#endregion

        //#region Mock getExchangeRates
        val testData: MutableList<RateCurrencyDomain> = arrayListOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("USD"), 0.534786),
            RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 0.563423)
        )

        Mockito.`when`(currencyRateRepo.getExchangeRates(testCurrency)).thenReturn(Single.just(testData))
        //#endregion

        currencyRateInteractor.getExchangeRates().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue(testData.sortedBy { it -> it.currency.displayName })
    }

    @Test
    fun testGetAvailableCurrencies()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        currencyRateInteractor.getAvailableCurrencies().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { !it.isNullOrEmpty() }
    }
}