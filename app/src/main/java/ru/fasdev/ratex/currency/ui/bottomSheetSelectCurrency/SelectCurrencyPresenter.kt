package ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

class SelectCurrencyPresenter @Inject constructor(val currencyBaseInteractor: CurrencyBaseInteractor) : MvpPresenter<SelectCurrencyView>() {
    var filterSearchCurrency: String? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        loadAvailableCurrencies()
    }

    fun searchCurrency(text: String) {
        filterSearchCurrency = text

        loadAvailableCurrencies()
    }

    fun loadAvailableCurrencies() {
        presenterScope.launch {
            try {
                val availableCurrencies = currencyBaseInteractor.getAvailableCurrencies()
                val baseCurrency = currencyBaseInteractor.getBaseCurrency()

                viewState.setListCurrency(
                    currencyBaseInteractor.filterSearchAvailbaleCurrenciesNameCode(availableCurrencies, filterSearchCurrency),
                    baseCurrency
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("ERROR", e.toString())
            }
        }
    }

    fun selectedCurrency(isChecked: Boolean, currencyDomain: CurrencyDomain) {
        if (isChecked) {
            currencyBaseInteractor.setBaseCurrency(currencyDomain)

            loadAvailableCurrencies()
        }
    }
}
