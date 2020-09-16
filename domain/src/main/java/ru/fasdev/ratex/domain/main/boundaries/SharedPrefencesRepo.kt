package ru.fasdev.ratex.domain.main.boundaries

interface SharedPrefencesRepo {
    fun getBaseCurrencyCode(): String?
    fun setBaseCurrencyCode(currencyCode: String?)
}