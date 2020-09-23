package ru.fasdev.ratex.domain.currencyRate.interactor

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyImageProvider
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.extension.toCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.text.DecimalFormat
import java.util.*

class CurrencyRateInteractorImpl(val currencyRateRepo: CurrencyRateRepo, val sharedPrefencesRepo: SharedPrefencesRepo, val currencyImageProvider: CurrencyImageProvider): CurrencyRateInteractor
{
    val arrayActualCodeCurrency = arrayListOf("CAD", "HKD", "ISK", "PHP", "DKK", "HUF", "CZK", "GBP", "RON", "SEK", "IDR", "INR", "BRL", "RUB", "HRK", "JPY", "THB", "CHF", "EUR", "MYR", "BGN", "TRY", "CNY", "NOK", "NZD", "ZAR", "USD", "MXN", "SGD", "AUD", "ILS", "KRW", "PLN")

    override fun getBaseCurrency(): Single<CurrencyDomain>
    {
        if (!sharedPrefencesRepo.getBaseCurrencyCode().isNullOrEmpty())
        {
            val baseCurrency = Currency.getInstance(sharedPrefencesRepo.getBaseCurrencyCode())
            return Single.just(baseCurrency.toCurrencyDomain())
        }
        else
        {
            return Single.just(Currency.getInstance(Locale.getDefault()).toCurrencyDomain())
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain)
    {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override fun getExchangeRates(): Single<List<RateCurrencyDomain>>
    {
        return  getBaseCurrency()
            .flatMap {
                currencyRateRepo.getExchangeRates(it.currencyCode)
                    .flatMapObservable { item -> Observable.fromIterable(item) }
                    .filter { item -> item.currency.currencyCode.toLowerCase() != it.currencyCode.toLowerCase() }
                    .toList()
            }
            .map { it -> it.sortedBy {
                it.currency.displayName
            } }
    }

    override fun getAvailableCurrencies(): Single<List<CurrencyDomain>>
    {
        return Observable.fromIterable(arrayActualCodeCurrency)
            .map { it -> CurrencyDomain.getInstance(it, currencyImageProvider) }
            .toList()
    }
}