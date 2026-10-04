package ru.fasdev.ratex.currency.ui.selectCurrency

import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

data class SelectCurrencyState(
    val searchText: String = "",
    val currencies: List<CurrencyDomain> = emptyList(),
    val baseCurrency: CurrencyDomain? = null
)
