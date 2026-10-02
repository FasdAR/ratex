package ru.fasdev.ratex.currency.domain.boundaries.interactor

import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

interface CurrencyBaseInteractor {
    suspend fun getBaseCurrency(): CurrencyDomain
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    suspend fun getAvailableCurrencies(): List<CurrencyDomain>
    fun filterSearchAvailbaleCurrenciesNameCode(list: List<CurrencyDomain>, nameCode: String?): List<CurrencyDomain>
}
