package ru.fasdev.ratex.domain.currency.interactor

import ru.fasdev.ratex.domain.currency.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class CurrencyRateInteractorImpl(val currencyRateRepo: CurrencyRateRepo) : CurrencyRateInteractor {
    override suspend fun getExchangeRates(): List<RateCurrencyDomain> = currencyRateRepo.getExchangeRates()
        .sortedBy {
            it.currency.displayName
        }
}
