package ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

@AddToEndSingle
interface ListCurrencyRateView : MvpView {
    fun setBaseCurrency(currency: String)
    fun setListExchangeRates(rateList: List<RateCurrencyDomain>)
    fun setRefreshingState(isRefreshing: Boolean)
    fun setNetworkError(message: String)
}
