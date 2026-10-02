package ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor

class ListCurrencyRatePresenter @Inject constructor(
    val currencyBaseInteractor: CurrencyBaseInteractor,
    val currencyRateInteractor: CurrencyRateInteractor
) : MvpPresenter<ListCurrencyRateView>() {
    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        getBaseCurrency()
        loadExchangeRates()
    }

    fun getBaseCurrency() {
        presenterScope.launch {
            try {
                viewState.setBaseCurrency(currencyBaseInteractor.getBaseCurrency().currencyCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TOOD: SEt NORMAL ERROR TO VIEW
                Log.e("ERROR", e.toString())
            }
        }
    }

    fun loadExchangeRates() {
        presenterScope.launch {
            viewState.setRefreshingState(true)

            try {
                viewState.setListExchangeRates(currencyRateInteractor.getExchangeRates())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO: CHANGE TO NORMAL MESSAGE IN CODE ... 400, 404 ....
                viewState.setNetworkError(e.message.toString())
            } finally {
                viewState.setRefreshingState(false)
            }
        }
    }
}
