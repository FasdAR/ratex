package ru.fasdev.ratex.core.data.sharedPrefences

import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo

class SharedPrefencesRepoImpl(val sPrefences: SPrefences) : SharedPrefencesRepo {
    override fun getBaseCurrencyCode(): String? = sPrefences.baseCurrencyCode

    override fun setBaseCurrencyCode(currencyCode: String?) {
        sPrefences.baseCurrencyCode = currencyCode
    }
}
