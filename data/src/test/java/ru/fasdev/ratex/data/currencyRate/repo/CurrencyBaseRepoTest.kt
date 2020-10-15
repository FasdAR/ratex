package ru.fasdev.ratex.data.currencyRate.repo

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.Mock
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

class CurrencyBaseRepoTest
{
    @get:Rule val mockitoJUnit = MockitoJUnit.rule()

    @Mock lateinit var sharedPrefencesRepo: SharedPrefencesRepo
    @Mock lateinit var currencyImageRepo: CurrencyImageRepo

    lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Before
    fun setUp() {
        currencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo)
    }

    @Test
    fun testGetBaseCurrencyNullPreferences()
    {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)

        val testLocale = Locale.US
        Locale.setDefault(testLocale)

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        currencyBaseRepo.getBaseCurrency().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue { it.currencyCode == Currency.getInstance(Locale.getDefault()).currencyCode }
    }

    @Test
    fun testGetBaseCurrency()
    {
        val testLocale = Locale.US
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(Currency.getInstance(testLocale).currencyCode)

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        currencyBaseRepo.getBaseCurrency().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue { it.currencyCode == Currency.getInstance(testLocale).currencyCode }
    }

    @Test
    fun testSetBaseCurrency()
    {
        val testCurrencyCode = "RUB"
        currencyBaseRepo.setBaseCurrency(CurrencyDomain.getInstance(testCurrencyCode))

        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode(testCurrencyCode)
    }

    @Test
    fun testGetAvailableCurrencies()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        currencyBaseRepo.getAvailableCurrencies().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue { !it.isNullOrEmpty() }
    }
}