package ru.fasdev.ratex.currency.ui.listCurrencyRate

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
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor

class ListCurrencyRateViewModel @Inject constructor(
    private val currencyBaseInteractor: CurrencyBaseInteractor,
    private val currencyRateInteractor: CurrencyRateInteractor
) : ViewModel() {
    private val _state = MutableStateFlow(ListCurrencyRateState())
    val state: StateFlow<ListCurrencyRateState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        onBaseCurrencyChanged()
    }

    fun onBaseCurrencyChanged() {
        loadBaseCurrency()
        onRefresh()
    }

    fun onRefresh() {
        // Предыдущий запрос отменяем, чтобы устаревший ответ не затёр курсы и не сбросил индикатор раньше времени
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                val rates = currencyRateInteractor.getExchangeRates()
                _state.update { it.copy(rates = rates, isRefreshing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO: CHANGE TO NORMAL MESSAGE IN CODE ... 400, 404 ....
                _state.update { it.copy(errorMessage = e.message.toString(), isRefreshing = false) }
            }
        }
    }

    fun onErrorShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadBaseCurrency() {
        viewModelScope.launch {
            try {
                val baseCurrency = currencyBaseInteractor.getBaseCurrency().currencyCode
                _state.update { it.copy(baseCurrency = baseCurrency) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO: SET NORMAL ERROR TO STATE
                Log.e("ERROR", e.toString())
            }
        }
    }
}
