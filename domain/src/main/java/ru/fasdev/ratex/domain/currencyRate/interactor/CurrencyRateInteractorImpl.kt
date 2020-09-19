package ru.fasdev.ratex.domain.currencyRate.interactor

import io.reactivex.Single
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.extension.toCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.text.DecimalFormat
import java.util.*

public class CurrencyRateInteractorImpl(
    val currencyRateRepo: CurrencyRateRepo,
    val sharedPrefencesRepo: SharedPrefencesRepo
): CurrencyRateInteractor
{
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
        return getBaseCurrency()
            .flatMap { currencyRateRepo.getExchangeRates(it.currencyCode) }
    }
}