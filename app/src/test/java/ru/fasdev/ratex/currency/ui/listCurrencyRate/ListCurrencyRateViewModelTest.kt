package ru.fasdev.ratex.currency.ui.listCurrencyRate

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import ru.fasdev.ratex.core.rule.MainDispatcherRule
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

class ListCurrencyRateViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val mockJunit = MockitoJUnit.rule()

    // #region Mock
    @Mock lateinit var currencyBaseInteractor: CurrencyBaseInteractor

    @Mock lateinit var currencyRateInteractor: CurrencyRateInteractor
    // #endregion

    // #region Test Data
    val testCurrency = CurrencyDomain.getInstance("USD")
    val testList = listOf(
        RateCurrencyDomain(CurrencyDomain.getInstance("RUB"), 77.0),
        RateCurrencyDomain(CurrencyDomain.getInstance("EUR"), 99.0)
    )
    // #endregion

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(testCurrency)
        Mockito.`when`(currencyRateInteractor.getExchangeRates()).thenReturn(testList)
    }

    private fun createViewModel() = ListCurrencyRateViewModel(currencyBaseInteractor, currencyRateInteractor)

    @Test
    fun testInitLoadsBaseCurrencyAndRates() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.state.value.baseCurrency).isEqualTo(testCurrency.currencyCode)
        assertThat(viewModel.state.value.rates).isEqualTo(testList)
        assertThat(viewModel.state.value.isRefreshing).isFalse()
        assertThat(viewModel.state.value.errorMessage).isNull()
    }

    @Test
    fun testBaseCurrencyChangedReloads() = runTest {
        val viewModel = createViewModel()
        val newCurrency = CurrencyDomain.getInstance("EUR")
        val newList = listOf(RateCurrencyDomain(testCurrency, 1.5))
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(newCurrency)
        Mockito.`when`(currencyRateInteractor.getExchangeRates()).thenReturn(newList)

        viewModel.onBaseCurrencyChanged()

        assertThat(viewModel.state.value.baseCurrency).isEqualTo(newCurrency.currencyCode)
        assertThat(viewModel.state.value.rates).isEqualTo(newList)
    }

    @Test
    fun testRefreshException() = runTest {
        val testMessage = "sdasd"
        val viewModel = createViewModel()
        Mockito.`when`(currencyRateInteractor.getExchangeRates())
            .thenAnswer { throw Exception(testMessage) }

        viewModel.onRefresh()

        assertThat(viewModel.state.value.errorMessage).isEqualTo(testMessage)
        assertThat(viewModel.state.value.isRefreshing).isFalse()
        assertThat(viewModel.state.value.rates).isEqualTo(testList)
    }

    @Test
    fun testErrorShownClearsError() = runTest {
        val viewModel = createViewModel()
        Mockito.`when`(currencyRateInteractor.getExchangeRates())
            .thenAnswer { throw Exception("error") }
        viewModel.onRefresh()

        viewModel.onErrorShown()

        assertThat(viewModel.state.value.errorMessage).isNull()
    }
}
