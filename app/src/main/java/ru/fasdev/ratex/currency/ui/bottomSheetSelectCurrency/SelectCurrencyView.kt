package ru.fasdev.ratex.currency.ui.bottomSheetSelectCurrency

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

@AddToEndSingle
interface SelectCurrencyView : MvpView {
    fun setListCurrency(list: List<CurrencyDomain>, baseCurrency: CurrencyDomain)
}
