package ru.fasdev.ratex.domain.currency.boundaries.interactor

import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

interface CurrencyBaseInteractor {
    suspend fun getBaseCurrency(): CurrencyDomain
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    suspend fun getAvailableCurrencies(): List<CurrencyDomain>
    fun filterSearchAvailbaleCurrenciesNameCode(list: List<CurrencyDomain>, nameCode: String?): List<CurrencyDomain>
}
