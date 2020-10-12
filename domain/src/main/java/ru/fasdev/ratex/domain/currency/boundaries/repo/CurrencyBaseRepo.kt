package ru.fasdev.ratex.domain.currency.boundaries.repo

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

interface CurrencyBaseRepo {
    fun getBaseCurrency(): Single<CurrencyDomain>
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    fun getAvailableCurrencies(): Single<List<CurrencyDomain>>
}