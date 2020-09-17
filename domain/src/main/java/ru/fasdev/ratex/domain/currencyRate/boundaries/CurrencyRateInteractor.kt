package ru.fasdev.ratex.domain.currencyRate.boundaries

import io.reactivex.Completable
import io.reactivex.Single
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import java.util.*

interface CurrencyRateInteractor
{
    fun getBaseCurrency(): Single<CurrencyDomain>
    fun setBaseCurrency(baseCurrency: CurrencyDomain)
    fun getExchangeRates(): Single<List<RateCurrencyDomain>>
}