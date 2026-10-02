package ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate

import java.lang.Exception
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.core.rule.MainDispatcherRule
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain
import ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate.ListCurrencyRatePresenter
import ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate.ListCurrencyRateView

class ListCurrencyRatePresenterTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val mockJunit = MockitoJUnit.rule()

    // #region Mock
    @Mock lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    @Mock lateinit var currencyRateInteractor: CurrencyRateInteractor

    @Mock lateinit var view: ListCurrencyRateView
    // #endregion

    lateinit var presenter: ListCurrencyRatePresenter

    // #region Test Data
    val testCurrency = CurrencyDomain.getInstance("USD")
    val testList = listOf(
        RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 77.0),
        RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 99.0)
    )
    // #endregion

    @Before
    fun setUp() = runTest {
        presenter = ListCurrencyRatePresenter(currencyBaseInteractor, currencyRateInteractor)

        // #region First Attach
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(testCurrency)
        Mockito.`when`(currencyRateInteractor.getExchangeRates()).thenReturn(testList)
        // #endregion

        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.detachView(view)
    }

    @Test
    fun testGetBaseCurrency() = runTest {
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(testCurrency)

        presenter.getBaseCurrency()

        Mockito.verify(view, times(2)).setBaseCurrency(testCurrency.currencyCode)
    }

    @Test
    fun testLoadExchangeRates() = runTest {
        Mockito.`when`(currencyRateInteractor.getExchangeRates()).thenReturn(testList)

        presenter.loadExchangeRates()

        Mockito.verify(view, times(2)).setListExchangeRates(testList)
    }

    @Test
    fun testLoadExchangeRatesException() = runTest {
        val testMessage = "sdasd"

        Mockito.`when`(currencyRateInteractor.getExchangeRates())
            .thenAnswer { throw Exception(testMessage) }

        presenter.loadExchangeRates()

        Mockito.verify(view).setNetworkError(testMessage)
    }
}
