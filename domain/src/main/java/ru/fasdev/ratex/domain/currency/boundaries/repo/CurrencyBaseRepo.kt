package ru.fasdev.ratex.domain.currency.boundaries.repo

import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

interface CurrencyBaseRepo {
    suspend fun getBaseCurrency(): CurrencyDomain
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    suspend fun getAvailableCurrencies(): List<CurrencyDomain>
}
