package ru.fasdev.ratex.domain.currency.interactor

import io.reactivex.Observable
import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyBaseInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain

class CurrencyBaseInteractorImpl(val currencyBaseRepo: CurrencyBaseRepo) : CurrencyBaseInteractor
{
    override fun getBaseCurrency(): Single<CurrencyDomain>
    {
        return currencyBaseRepo.getBaseCurrency()
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain)
    {
        currencyBaseRepo.setBaseCurrency(baseCurrency)
    }

    override fun getAvailableCurrencies(): Single<List<CurrencyDomain>>
    {
        return currencyBaseRepo.getAvailableCurrencies()
            .map { it -> it.sortedBy {
                it.displayName
            } }
    }

    override fun filterSearchAvailbaleCurrenciesNameCode(list: List<CurrencyDomain>, nameCode: String?): List<CurrencyDomain>
    {
        if (nameCode.isNullOrEmpty()) {
            return list
        }
        else
        {
            val name = nameCode.toLowerCase()

            if (name.length > 2)
            {
                return list.filter { it -> it.displayName.toLowerCase().contains(name) || it.currencyCode.toLowerCase().contains(name) }
            }
            return emptyList()
        }
    }
}