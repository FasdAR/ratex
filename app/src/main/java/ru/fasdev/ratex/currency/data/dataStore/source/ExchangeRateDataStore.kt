package ru.fasdev.ratex.currency.data.dataStore.source

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.api.ExchangeRateApi
import ru.fasdev.ratex.currency.data.dataStore.CurrencyRateDataStore
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain

class ExchangeRateDataStore(
    val exchangeRateApi: ExchangeRateApi,
    val imageRepo: CurrencyImageRepo,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurrencyRateDataStore {
    override suspend fun getExchangeRates(baseCurrency: CurrencyDomain): List<RateCurrencyDomain> = withContext(ioDispatcher) {
        exchangeRateApi
            .getProducts(baseCurrency.currencyCode)
            .rates
            .entries
            .map {
                RateCurrencyDomain(CurrencyDomain.getInstance(it.key, imageRepo), it.value)
            }
            .filter {
                it.currency.currencyCode != baseCurrency.currencyCode
            }
    }
}
