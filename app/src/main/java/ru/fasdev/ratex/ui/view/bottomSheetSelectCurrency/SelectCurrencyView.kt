package ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain

@AddToEndSingle
interface SelectCurrencyView: MvpView
{
    fun setListCurrency(list: List<CurrencyDomain>)
}