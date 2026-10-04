package ru.fasdev.ratex.currency.ui.selectCurrency

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
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class SelectCurrencyViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val mockJunit = MockitoJUnit.rule()

    // #region Mock
    @Mock lateinit var currencyBaseInteractor: CurrencyBaseInteractor
    // #endregion

    // #region Test Data
    val testCurrency = CurrencyDomain.getInstance("USD")
    val testList = listOf(
        CurrencyDomain.getInstance("RUB"),
        CurrencyDomain.getInstance("EUR")
    )
    // #endregion

    lateinit var viewModel: SelectCurrencyViewModel

    @Before
    fun setUp() = runTest {
        Mockito.`when`(currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(testList, "")).thenReturn(testList)
        Mockito.`when`(currencyBaseInteractor.getBaseCurrency()).thenReturn(testCurrency)
        Mockito.`when`(currencyBaseInteractor.getAvailableCurrencies()).thenReturn(testList)

        viewModel = SelectCurrencyViewModel(currencyBaseInteractor)
    }

    @Test
    fun testOpenedLoadsCurrencies() = runTest {
        viewModel.onOpened()

        assertThat(viewModel.state.value.currencies).isEqualTo(testList)
        assertThat(viewModel.state.value.baseCurrency).isEqualTo(testCurrency)
    }

    @Test
    fun testSearchCurrency() = runTest {
        val testSearchList = listOf(CurrencyDomain.getInstance("RUB"))
        Mockito.`when`(currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(testList, "rub"))
            .thenReturn(testSearchList)

        viewModel.onSearchChanged("rub")

        assertThat(viewModel.state.value.searchText).isEqualTo("rub")
        assertThat(viewModel.state.value.currencies).isEqualTo(testSearchList)
    }

    @Test
    fun testOpenedResetsSearch() = runTest {
        viewModel.onSearchChanged("rub")

        viewModel.onOpened()

        assertThat(viewModel.state.value.searchText).isEmpty()
    }

    @Test
    fun testCurrencySelected() = runTest {
        val selectedCurrency = CurrencyDomain.getInstance("RUB")

        viewModel.onCurrencySelected(selectedCurrency)

        Mockito.verify(currencyBaseInteractor).setBaseCurrency(selectedCurrency)
        assertThat(viewModel.state.value.currencies).isEqualTo(testList)
    }
}
