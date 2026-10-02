package ru.fasdev.ratex.currency.domain.boundaries.repo

import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

interface CurrencyBaseRepo {
    suspend fun getBaseCurrency(): CurrencyDomain
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    suspend fun getAvailableCurrencies(): List<CurrencyDomain>
}
