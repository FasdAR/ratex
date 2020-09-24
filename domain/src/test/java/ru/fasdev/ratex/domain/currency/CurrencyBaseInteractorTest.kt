package ru.fasdev.ratex.domain.currency

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currency.interactor.CurrencyBaseInteractorImpl
import ru.fasdev.ratex.domain.currency.interactor.CurrencyRateInteractorImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

class CurrencyBaseInteractorTest
{
    private lateinit var currencyBaseRepo: CurrencyBaseRepo

    private lateinit var currencyBaseInteractor: CurrencyBaseInteractor
    private lateinit var sharedPrefencesRepo: SharedPrefencesRepo

    @Before
    fun setUp() {
        currencyBaseRepo = Mockito.mock(CurrencyBaseRepo::class.java)
        sharedPrefencesRepo = Mockito.mock(SharedPrefencesRepo::class.java)

        currencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)
    }

    @Test
    fun testGetBaseCurrency()
    {
        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        val testedCurrencyCode = "RUB"

        //#region Mock getBaseCurrencyCode and getDefaultLocale
        val testCurrency = CurrencyDomain.getInstance(testedCurrencyCode)
        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(Single.just(testCurrency))
        Locale.setDefault(Locale.US)
        //#endregion

        currencyBaseInteractor.getBaseCurrency().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it -> it.currencyCode == testCurrency.currencyCode }
    }

    @Test
    fun testSetBaseCurrency()
    {
        val testCurrency = CurrencyDomain.getInstance("RUB")

        currencyBaseInteractor.setBaseCurrency(testCurrency)

        Mockito.verify(currencyBaseRepo).setBaseCurrency(testCurrency)
    }

    @Test
    fun testGetAvailableCurrencies()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        Mockito.`when`(currencyBaseRepo.getAvailableCurrencies()).thenReturn(Single.just(testData))

        currencyBaseInteractor.getAvailableCurrencies().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue(testData.sortedBy { it.displayName })
    }

    @Test
    fun testSearchAvailableCurrenciesCode()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        Mockito.`when`(currencyBaseRepo.getAvailableCurrencies()).thenReturn(Single.just(testData))

        currencyBaseInteractor.searchAvailableCurrenciesNameCode(findCurrency.currencyCode).subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue{ it-> it.get(0).displayName == findCurrency.displayName}
    }

    @Test
    fun testSearchAvailableCurrenciesName()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        Mockito.`when`(currencyBaseRepo.getAvailableCurrencies()).thenReturn(Single.just(testData))

        currencyBaseInteractor.searchAvailableCurrenciesNameCode(findCurrency.displayName.substring(0, 3)).subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue{ it-> it.get(0).displayName == findCurrency.displayName}
    }

    @Test
    fun testSearchAvailableCurrenciesNull()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        Mockito.`when`(currencyBaseRepo.getAvailableCurrencies()).thenReturn(Single.just(testData))

        currencyBaseInteractor.searchAvailableCurrenciesNameCode(findCurrency.displayName.substring(0, 2)).subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue{ it-> it.isNullOrEmpty()}
    }
}