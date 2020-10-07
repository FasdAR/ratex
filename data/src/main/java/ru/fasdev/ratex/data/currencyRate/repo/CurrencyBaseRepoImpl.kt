package ru.fasdev.ratex.data.currencyRate.repo

import io.reactivex.Observable
import io.reactivex.Single
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyBaseRepo
import ru.fasdev.ratex.domain.currency.boundaries.repo.CurrencyImageRepo
import ru.fasdev.ratex.domain.currency.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currency.entity.extension.toCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

class CurrencyBaseRepoImpl(val sharedPrefencesRepo: SharedPrefencesRepo, val currencyImageRepo: CurrencyImageRepo) : CurrencyBaseRepo
{
    private val arrayActualCodeCurrency = arrayListOf("CAD", "HKD", "ISK", "PHP", "DKK", "HUF", "CZK", "GBP", "RON", "SEK", "IDR", "INR", "BRL", "RUB", "HRK", "JPY", "THB", "CHF", "EUR", "MYR", "BGN", "TRY", "CNY", "NOK", "NZD", "ZAR", "USD", "MXN", "SGD", "AUD", "ILS", "KRW", "PLN")
    private var listAvailableCurrencies: List<CurrencyDomain>? = null

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

    override fun getAvailableCurrencies(): Single<List<CurrencyDomain>>
    {
        if (listAvailableCurrencies.isNullOrEmpty())
        {
            return Observable.fromIterable(arrayActualCodeCurrency)
                .map { it -> CurrencyDomain.getInstance(it, currencyImageRepo) }
                .toList()
                .doOnSuccess { listAvailableCurrencies = it }
        }
        else
        {
            return Single.just(listAvailableCurrencies)
        }
    }
}