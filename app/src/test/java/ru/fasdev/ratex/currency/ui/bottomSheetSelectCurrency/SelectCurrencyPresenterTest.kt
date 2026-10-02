package ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency

import kotlin.text.Typography.times
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
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class SelectCurrencyPresenterTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val mockJunit = MockitoJUnit.rule()

    //region Mock
    @Mock lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    @Mock lateinit var view: SelectCurrencyView
    // #endregion

    lateinit var presenter: SelectCurrencyPresenter

    // #region Test Data
    val testCurrency = CurrencyDomain.getInstance("USD")
    val testList = listOf(
        CurrencyDomain.getInstance("RUB"),
        CurrencyDomain.getInstance("EUR")
    )
    // #endregion

    @Before
    fun setUp() = runTest {
        presenter = SelectCurrencyPresenter(currencyBaseInteractor)

        // #region First Attach
        Mockito.`when`(
            currencyBaseInteractor
                .filterSearchAvailbaleCurrenciesNameCode(testList, null)
        )
            .thenReturn(testList)

        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(testCurrency)
        Mockito.`when`(currencyBaseInteractor.getAvailableCurrencies()).thenReturn(testList)
        // #endregion

        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.detachView(view)
    }

    @Test
    fun testSearchCurrency() = runTest {
        val testSearchList = listOf(
            CurrencyDomain.getInstance("RUB")
        )
        val testText: String = "rub"

        Mockito.`when`(
            currencyBaseInteractor
                .filterSearchAvailbaleCurrenciesNameCode(testList, testText)
        )
            .thenReturn(testSearchList)

        presenter.searchCurrency("rub")

        Mockito.verify(view, times(1))
            .setListCurrency(testSearchList, testCurrency)
    }

    @Test
    fun testLoadAvailableCurrencies() = runTest {
        presenter.loadAvailableCurrencies()

        Mockito.verify(view, times(2))
            .setListCurrency(testList, testCurrency)
    }

    @Test
    fun testSelectedCurrency() = runTest {
        val selectedCurrency = CurrencyDomain.getInstance("RUB")
        presenter.selectedCurrency(true, selectedCurrency)

        Mockito.verify(currencyBaseInteractor).setBaseCurrency(selectedCurrency)

        Mockito.verify(view, times(2))
            .setListCurrency(testList, testCurrency)
    }
}
