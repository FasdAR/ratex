package ru.fasdev.ratex.currency.ui.selectCurrency

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class SelectCurrencyViewModel @Inject constructor(private val currencyBaseInteractor: CurrencyBaseInteractor) : ViewModel() {
    private val _state = MutableStateFlow(SelectCurrencyState())
    val state: StateFlow<SelectCurrencyState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Вызывается при каждом открытии sheet: ViewModel живёт дольше sheet, поэтому поиск сбрасывается здесь. */
    fun onOpened() {
        _state.update { it.copy(searchText = "") }
        loadAvailableCurrencies()
    }

    fun onSearchChanged(text: String) {
        _state.update { it.copy(searchText = text) }
        loadAvailableCurrencies()
    }

    fun onCurrencySelected(currency: CurrencyDomain) {
        currencyBaseInteractor.setBaseCurrency(currency)
        loadAvailableCurrencies()
    }

    private fun loadAvailableCurrencies() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val availableCurrencies = currencyBaseInteractor.getAvailableCurrencies()
                val baseCurrency = currencyBaseInteractor.getBaseCurrency()
                val filtered = currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(
                    availableCurrencies,
                    _state.value.searchText
                )

                _state.update { it.copy(currencies = filtered, baseCurrency = baseCurrency) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("ERROR", e.toString())
            }
        }
    }
}
