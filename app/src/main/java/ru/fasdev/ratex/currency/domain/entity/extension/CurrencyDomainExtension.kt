package ru.fasdev.ratex.currency.domain.entity.extension

import java.util.*
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

fun Currency.toCurrencyDomain(): CurrencyDomain = CurrencyDomain(currencyCode, symbol, displayName, null)
fun Currency.toCurrencyDomain(urlImage: String?) = CurrencyDomain(currencyCode, symbol, displayName, urlImage)

fun isKnownCurrencyCode(code: String): Boolean = runCatching { Currency.getInstance(code) }.isSuccess
