package ru.fasdev.ratex.currency.data.repo

import ru.fasdev.ratex.currency.data.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

class CurrencyRateRepoImpl(val currencyRateDataStore: CurrencyRateDataStore, val currencyBaseRepo: CurrencyBaseRepo) :
    CurrencyRateRepo {
    override suspend fun getExchangeRates(): List<RateCurrencyDomain> =
        currencyRateDataStore.getExchangeRates(currencyBaseRepo.getBaseCurrency())
}
