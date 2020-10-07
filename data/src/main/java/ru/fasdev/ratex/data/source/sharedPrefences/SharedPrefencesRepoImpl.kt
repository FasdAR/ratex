package ru.fasdev.ratex.data.source.sharedPrefences

import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

class SharedPrefencesRepoImpl (val sPrefences: SPrefences): SharedPrefencesRepo
{
    override fun getBaseCurrencyCode(): String?
    {
        return sPrefences.baseCurrencyCode
    }

    override fun setBaseCurrencyCode(currencyCode: String?)
    {
        sPrefences.baseCurrencyCode = currencyCode
    }
}