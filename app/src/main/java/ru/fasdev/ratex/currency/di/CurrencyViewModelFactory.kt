package ru.fasdev.ratex.currency.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider
import ru.fasdev.ratex.currency.di.scope.CurrencyScope
import ru.fasdev.ratex.currency.ui.listCurrencyRate.ListCurrencyRateViewModel
import ru.fasdev.ratex.currency.ui.selectCurrency.SelectCurrencyViewModel

@CurrencyScope
class CurrencyViewModelFactory @Inject constructor(
    private val listCurrencyRate: Provider<ListCurrencyRateViewModel>,
    private val selectCurrency: Provider<SelectCurrencyViewModel>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        ListCurrencyRateViewModel::class.java -> listCurrencyRate.get()
        SelectCurrencyViewModel::class.java -> selectCurrency.get()
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
