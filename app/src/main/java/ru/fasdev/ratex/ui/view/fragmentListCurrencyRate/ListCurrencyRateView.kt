package ru.fasdev.ratex.ui.view.fragmentListCurrencyRate

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain

@AddToEndSingle
interface ListCurrencyRateView: MvpView
{
    fun setBaseCurrency(currency: String)
    fun setListExchangeRates(rateList: List<RateCurrencyDomain>)
    fun setRefreshingState(isRefreshing: Boolean)
    fun setNetworkError(message: String)
}