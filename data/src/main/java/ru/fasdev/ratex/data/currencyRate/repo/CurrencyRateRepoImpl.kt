package ru.fasdev.ratex.data.currencyRate.repo

import io.reactivex.Single
import ru.fasdev.ratex.data.currencyRate.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.domain.currency.entity.RateCurrencyDomain

class CurrencyRateRepoImpl(val currencyRateDataStore: CurrencyRateDataStore, val currencyBaseRepo: CurrencyBaseRepo) :
    CurrencyRateRepo {
    override fun getExchangeRates(): Single<List<RateCurrencyDomain>> = currencyBaseRepo.getBaseCurrency()
        .flatMap {
            currencyRateDataStore.getExchangeRates(it)
        }
}
