package ru.fasdev.ratex.main.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.fasdev.ratex.currency.ui.listCurrencyRate.ListCurrencyRateRoute
import ru.fasdev.ratex.currency.ui.listCurrencyRate.ListCurrencyRateViewModel
import ru.fasdev.ratex.currency.ui.selectCurrency.SelectCurrencyViewModel

@Serializable
data object ListCurrencyRateKey : NavKey

@Composable
fun MainNavigation(viewModelFactory: ViewModelProvider.Factory) {
    val backStack = rememberNavBackStack(ListCurrencyRateKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ListCurrencyRateKey> {
                ListCurrencyRateRoute(
                    viewModel = viewModel<ListCurrencyRateViewModel>(factory = viewModelFactory),
                    selectCurrencyViewModel = viewModel<SelectCurrencyViewModel>(factory = viewModelFactory)
                )
            }
        }
    )
}
