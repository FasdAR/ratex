package ru.fasdev.ratex.domain.currencyRate.interactor

import ru.fasdev.ratex.domain.currency.boundaries.CurrencyInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

public class CurrencyRateInteractorImpl (val currencyInteractor: CurrencyInteractor, val currencyRateRepo: CurrencyRateRepo, val sharedPrefencesRepo: SharedPrefencesRepo): CurrencyRateInteractor
{
    override fun getBaseCurrency(): CurrencyDomain
    {
        if (!sharedPrefencesRepo.getBaseCurrencyCode().isNullOrEmpty())
        {
            val baseCurrency = Currency.getInstance(sharedPrefencesRepo.getBaseCurrencyCode())
            return currencyInteractor.mapCurrencyToCurrencyDomain(baseCurrency)
        }
        else
        {
            return currencyInteractor.mapCurrencyToCurrencyDomain(Currency.getInstance(Locale.getDefault()))
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain)
    {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override fun getExchangeRates(): List<RateCurrencyDomain>
    {
        return currencyRateRepo.getExchangeRates(getBaseCurrency().currencyCode)
    }
}