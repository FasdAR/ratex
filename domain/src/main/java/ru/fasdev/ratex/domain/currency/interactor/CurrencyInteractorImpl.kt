package ru.fasdev.ratex.domain.currency.interactor

import ru.fasdev.ratex.domain.currency.boundaries.CurrencyInteractor
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import java.util.*

class CurrencyInteractorImpl : CurrencyInteractor
{
    override fun getDomainCurrencyByCode(code: String): CurrencyDomain
    {
        return mapCurrencyToCurrencyDomain(Currency.getInstance(code))
    }

    //TODO: EDIT TO NORMAL MAPPER
    override fun mapCurrencyToCurrencyDomain(currency: Currency): CurrencyDomain
    {
        return CurrencyDomain(currency.currencyCode, currency.symbol, currency.displayName)
    }
}