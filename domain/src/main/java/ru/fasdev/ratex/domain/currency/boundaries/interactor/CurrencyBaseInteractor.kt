package ru.fasdev.ratex.domain.currency.boundaries.interactor

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

interface CurrencyBaseInteractor
{
    fun getBaseCurrency(): Single<CurrencyDomain>
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    fun getAvailableCurrencies(): Single<List<CurrencyDomain>>
    fun filterSearchAvailbaleCurrenciesNameCode(list: List<CurrencyDomain>, nameCode: String?): List<CurrencyDomain>
}