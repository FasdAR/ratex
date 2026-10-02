package ru.fasdev.ratex.currency.domain.interactor

import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

class CurrencyRateInteractorImpl(val currencyRateRepo: CurrencyRateRepo) : CurrencyRateInteractor {
    override suspend fun getExchangeRates(): List<RateCurrencyDomain> = currencyRateRepo.getExchangeRates()
        .sortedBy {
            it.currency.displayName
        }
}
