package ru.fasdev.ratex.domain.currency.interactor

import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class CurrencyRateInteractorImpl(val currencyRateRepo: CurrencyRateRepo): CurrencyRateInteractor
{
    override fun getExchangeRates(): Single<List<RateCurrencyDomain>>
    {
        return currencyRateRepo.getExchangeRates()
            .map { it -> it.sortedBy {
                it.currency.displayName
            } }
    }
}