package ru.fasdev.ratex.currency.domain.interactor

import ru.fasdev.ratex.currency.domain.boundaries.interactor.CurrencyRateInteractor
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.extension.isKnownCurrencyCode

class CurrencyRateInteractorImpl(
    val currencyRateRepo: CurrencyRateRepo,
    val currencyBaseRepo: CurrencyBaseRepo,
    val currencyImageRepo: CurrencyImageRepo
) : CurrencyRateInteractor {
    override suspend fun getExchangeRates(): List<RateCurrencyDomain> {
        val snapshot = currencyRateRepo.getSnapshot()
        val baseCurrency = currencyBaseRepo.getBaseCurrency()

        return snapshot
            .crossRates(baseCurrency.currencyCode)
            .filterKeys { isKnownCurrencyCode(it) }
            .map { RateCurrencyDomain(CurrencyDomain.getInstance(it.key, currencyImageRepo), it.value) }
            .sortedBy { it.currency.displayName }
    }
}
