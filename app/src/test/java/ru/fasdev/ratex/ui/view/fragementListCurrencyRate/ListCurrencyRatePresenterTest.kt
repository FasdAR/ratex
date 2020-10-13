package ru.fasdev.ratex.ui.view.fragementListCurrencyRate

import androidx.annotation.NonNull
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler.ExecutorWorker
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import retrofit2.HttpException
import retrofit2.Response
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRatePresenter
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.`ListCurrencyRateView$$State`
import java.lang.Exception
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


class ListCurrencyRatePresenterTest
{
    lateinit var currencyBaseInteractor: CurrencyBaseInteractor
    lateinit var currencyRateInteractor: CurrencyRateInteractor

    lateinit var view: `ListCurrencyRateView$$State`
    lateinit var presenter: ListCurrencyRatePresenter

    @Before
    fun setUp() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        view = Mockito.mock(`ListCurrencyRateView$$State`::class.java)

        currencyBaseInteractor = Mockito.mock(CurrencyBaseInteractor::class.java)
        currencyRateInteractor = Mockito.mock(CurrencyRateInteractor::class.java)

        presenter = ListCurrencyRatePresenter(currencyBaseInteractor, currencyRateInteractor)
        presenter.setViewState(view)
    }

    @Test
    fun testGetBaseCurrency()
    {
        val testCurrency = CurrencyDomain.getInstance("USD")

        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(Single.just(testCurrency))

        val testObserver: TestObserver<CurrencyDomain> = TestObserver()
        currencyBaseInteractor.getBaseCurrency().subscribe(testObserver)

        presenter.getBaseCurrency()

        Mockito.verify(view).setBaseCurrency(testCurrency.currencyCode)
    }

    @Test
    fun testLoadExchangeRates()
    {
        val testList = listOf(
            RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 77.0),
            RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 99.0)
        )

        Mockito.`when`(currencyRateInteractor.getExchangeRates()).thenReturn(Single.just(testList))

        presenter.loadExchangeRates()

        Mockito.verify(view).setRefreshingState(true)
        Mockito.verify(view).setRefreshingState(false)
        Mockito.verify(view).setListExchangeRates(testList)
    }

    @Test
    fun testLoadExchangeRatesException()
    {
        val testMessage = "sdasd"

        Mockito.`when`(currencyRateInteractor.getExchangeRates())
            .thenReturn(Single.error(Exception(testMessage)))

        presenter.loadExchangeRates()

        Mockito.verify(view).setRefreshingState(true)
        Mockito.verify(view).setRefreshingState(false)
        Mockito.verify(view).setNetworkError(testMessage)
    }
}