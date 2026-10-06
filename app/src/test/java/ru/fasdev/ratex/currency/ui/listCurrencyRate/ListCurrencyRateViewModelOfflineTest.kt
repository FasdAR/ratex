package ru.fasdev.ratex.currency.ui.listCurrencyRate

import android.os.Build
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.fasdev.ratex.core.rule.MainDispatcherRule
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

/** Отдельный класс на Robolectric: при ошибке загрузки базы ViewModel пишет в android.util.Log, а в обычных unit-тестах он не замокан. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ListCurrencyRateViewModelOfflineTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val mockJunit = MockitoJUnit.rule()

    @Mock lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    @Mock lateinit var currencyRateInteractor: CurrencyRateInteractor

    private val testCurrency = CurrencyDomain.getInstance("USD")
    private val testList = listOf(RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 99.0))

    @Test
    fun testRefreshRecoversBaseCurrencyAfterFailedInitialLoad() = runTest {
        // первый запуск без сети: и база, и курсы падают, потом сеть появляется
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency())
            .thenAnswer { throw Exception("no network") }
            .thenReturn(testCurrency)
        Mockito.`when`(currencyRateInteractor.getExchangeRates())
            .thenAnswer { throw Exception("no network") }
            .thenReturn(testList)
        val viewModel = ListCurrencyRateViewModel(currencyBaseInteractor, currencyRateInteractor)
        assertThat(viewModel.state.value.baseCurrency).isNull()

        viewModel.onRefresh()

        assertThat(viewModel.state.value.baseCurrency).isEqualTo(testCurrency.currencyCode)
        assertThat(viewModel.state.value.rates).isEqualTo(testList)
    }
}
