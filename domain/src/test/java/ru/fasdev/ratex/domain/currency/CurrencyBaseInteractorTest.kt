package ru.fasdev.ratex.domain.currency

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.interactor.CurrencyBaseInteractorImpl

class CurrencyBaseInteractorTest {
    @get:Rule val mockitoJunit = MockitoJUnit.rule()

    @Mock private lateinit var currencyBaseRepo: CurrencyBaseRepo

    private lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    @Before
    fun setUp() {
        currencyBaseInteractor = CurrencyBaseInteractorImpl(currencyBaseRepo)
    }

    @Test
    fun testGetBaseCurrency() {
        // #region Mock getBaseCurrencyCode and getDefaultLocale
        val testCurrency = CurrencyDomain.getInstance("RUB")
        Locale.setDefault(Locale.US)

        Mockito.`when`(currencyBaseRepo.getBaseCurrency()).thenReturn(Single.just(testCurrency))
        // #endregion

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()

        currencyBaseInteractor.getBaseCurrency().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue { it.currencyCode == testCurrency.currencyCode }
    }

    @Test
    fun testSetBaseCurrency() {
        val testCurrency = CurrencyDomain.getInstance("RUB")

        currencyBaseInteractor.setBaseCurrency(testCurrency)

        Mockito.verify(currencyBaseRepo).setBaseCurrency(testCurrency)
    }

    @Test
    fun testGetAvailableCurrencies() {
        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        Mockito.`when`(currencyBaseRepo.getAvailableCurrencies()).thenReturn(Single.just(testData))

        val testObserver: TestObserver<List<CurrencyDomain>> = TestObserver()

        currencyBaseInteractor.getAvailableCurrencies().subscribe(testObserver)

        testObserver
            .assertComplete()
            .assertValue(testData.sortedBy { it.displayName })
    }

    @Test
    fun testSearchAvailableCurrenciesByCode() {
        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        val result: List<CurrencyDomain> = currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(
            testData,
            findCurrency.currencyCode
        )

        assertThat(result)
            .isNotNull()
            .isNotEmpty()

        assertThat(result.get(0))
            .extracting { it.currencyCode }
            .isEqualTo(findCurrency.currencyCode)
    }

    @Test
    fun testSearchAvailableCurrenciesByName() {
        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        val result = currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(testData, findCurrency.displayName.substring(0, 3))

        assertThat(result)
            .isNotNull()
            .isNotEmpty()

        assertThat(result.get(0))
            .extracting { it.currencyCode }
            .isEqualTo(findCurrency.currencyCode)
    }

    @Test
    fun testSearchAvailableCurrenciesNull() {
        val findCurrency: CurrencyDomain = CurrencyDomain.getInstance("EUR")

        val testData: MutableList<CurrencyDomain> = arrayListOf(
            CurrencyDomain.getInstance("RUB"),
            CurrencyDomain.getInstance("USD"),
            CurrencyDomain.getInstance("EUR")
        )

        val result = currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(testData, findCurrency.displayName.substring(0, 2))

        assertThat(result)
            .isNullOrEmpty()
    }
}
