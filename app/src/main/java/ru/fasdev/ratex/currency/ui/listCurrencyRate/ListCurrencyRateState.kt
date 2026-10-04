package ru.fasdev.ratex.currency.ui.listCurrencyRate

import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

data class ListCurrencyRateState(
    val baseCurrency: String? = null,
    val rates: List<RateCurrencyDomain> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
