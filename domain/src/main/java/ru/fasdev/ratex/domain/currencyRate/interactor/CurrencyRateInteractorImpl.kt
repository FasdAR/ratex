package ru.fasdev.ratex.domain.currencyRate.interactor

import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateInteractor
import ru.fasdev.ratex.domain.currencyRate.boundaries.CurrencyRateRepo
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import ru.fasdev.ratex.domain.currencyRate.entity.RateCurrencyDomain
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import java.util.*

public class CurrencyRateInteractorImpl (val currencyRateRepo: CurrencyRateRepo, val sharedPrefencesRepo: SharedPrefencesRepo): CurrencyRateInteractor
{
    override fun getBaseCurrency(): CurrencyDomain
    {
        if (sharedPrefencesRepo.getBaseCurrencyCode().isNullOrEmpty())
        {
            val currentLocale = Locale.getDefault()
            val currentCurrency = Currency.getInstance(currentLocale)

            sharedPrefencesRepo.setBaseCurrencyCode(currentCurrency.currencyCode)

            return mapCurrencyToCurrencyDomain(currentCurrency)
        }
        else
        {
            val baseCurrency = Currency.getInstance(sharedPrefencesRepo.getBaseCurrencyCode())
            return mapCurrencyToCurrencyDomain(baseCurrency)
        }
    }

    override fun setBaseCurrency(baseCurrency: CurrencyDomain)
    {
        sharedPrefencesRepo.setBaseCurrencyCode(baseCurrency.currencyCode)
    }

    override fun getExchangeRates(): List<RateCurrencyDomain>
    {
        return currencyRateRepo.getExchangeRates(sharedPrefencesRepo.getBaseCurrencyCode().toString())
    }

    //TODO: EDIT TO NORMAL MAPPER
    private fun mapCurrencyToCurrencyDomain(currency: Currency): CurrencyDomain
    {
        return CurrencyDomain(currency.currencyCode, currency.symbol, currency.displayName)
    }
}