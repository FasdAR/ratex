package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import org.mockito.Mockito
import org.mockito.Mockito.times
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import ru.fasdev.ratex.rule.InitScheduler
import kotlin.text.Typography.times

class SelectCurrencyPresenterTest
{
    @get:Rule
    val InitScheduler = InitScheduler()

    lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    lateinit var view: SelectCurrencyView
    lateinit var presenter: SelectCurrencyPresenter

    //#region Test Data
    val testCurrency = CurrencyDomain.getInstance("USD")
    val testList = listOf(
        CurrencyDomain.getInstance("RUB"),
        CurrencyDomain.getInstance("EUR")
    )
    //#endregion

    @Before
    fun setUp() {
        view = Mockito.mock(SelectCurrencyView::class.java)

        currencyBaseInteractor = Mockito.mock(CurrencyBaseInteractor::class.java)

        presenter = SelectCurrencyPresenter(currencyBaseInteractor)

        //#region First Attach
        Mockito.`when`(currencyBaseInteractor
            .filterSearchAvailbaleCurrenciesNameCode(testList, null))
            .thenReturn(testList)

        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(Single.just(testCurrency))
        Mockito.`when`(currencyBaseInteractor.getAvailableCurrencies()).thenReturn(Single.just(testList))
        //#endregion

        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.detachView(view)
    }

    @Test
    fun testSearchCurrency() {
        val testSearchList = listOf(
            CurrencyDomain.getInstance("RUB")
        )
        val testText: String = "rub"

        Mockito.`when`(currencyBaseInteractor
            .filterSearchAvailbaleCurrenciesNameCode(testList, testText))
            .thenReturn(testSearchList)

        presenter.searchCurrency("rub")

        Mockito.verify(view, times(1))
            .setListCurrency(testSearchList, testCurrency)
    }

    @Test
    fun testLoadAvailableCurrencies() {
        presenter.loadAvailableCurrencies()

        Mockito.verify(view, times(2))
            .setListCurrency(testList, testCurrency)
    }

    @Test
    fun testSelectedCurrency() {
        val selectedCurrency = CurrencyDomain.getInstance("RUB")
        presenter.selectedCurrency(true, selectedCurrency)

        Mockito.verify(currencyBaseInteractor).setBaseCurrency(selectedCurrency)

        Mockito.verify(view, times(2))
            .setListCurrency(testList, testCurrency)
    }
}