package ru.fasdev.ratex.core.domain.boundaries

interface SharedPrefencesRepo {
    fun getBaseCurrencyCode(): String?
    fun setBaseCurrencyCode(currencyCode: String?)
}
