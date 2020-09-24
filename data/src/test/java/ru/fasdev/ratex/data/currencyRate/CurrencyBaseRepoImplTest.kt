package ru.fasdev.ratex.data.currencyRate

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

class CurrencyBaseRepoImplTest
{
    lateinit var sharedPrefencesRepo: SharedPrefencesRepo
    lateinit var currencyImageRepo: CurrencyImageRepo

    lateinit var currencyBaseRepo: CurrencyBaseRepo

    @Before
    fun setUp() {
        sharedPrefencesRepo = Mockito.mock(SharedPrefencesRepo::class.java)
        currencyImageRepo = Mockito.mock(CurrencyImageRepo::class.java)

        //Mockito.`when`(currencyImageRepo.getImageUrl("CAD")).thenReturn(null)

        currencyBaseRepo = CurrencyBaseRepoImpl(sharedPrefencesRepo, currencyImageRepo)
    }

    @Test
    fun testGetBaseCurrencyNull()
    {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(null)

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()
        Locale.setDefault(Locale.US)

        currencyBaseRepo.getBaseCurrency().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it -> it.currencyCode == "USD" }
    }

    @Test
    fun testGetBaseCurrencyNotNull()
    {
        Mockito.`when`(sharedPrefencesRepo.getBaseCurrencyCode()).thenReturn(Currency.getInstance(Locale.US).currencyCode)

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        currencyBaseRepo.getBaseCurrency().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it -> it.currencyCode == "USD" }
    }

    @Test
    fun testSetBaseCurrency()
    {
        currencyBaseRepo.setBaseCurrency(CurrencyDomain.getInstance("RUB"))
        Mockito.verify(sharedPrefencesRepo).setBaseCurrencyCode("RUB")
    }

    @Test
    fun testGetAvailableCurrencies()
    {
        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        currencyBaseRepo.getAvailableCurrencies().subscribe(testObserver)

        testObserver.assertComplete()
        testObserver.assertValue { it-> !it.isNullOrEmpty() }
        testObserver.assertValue { it-> it.get(0).currencyCode == "CAD" }
    }
}