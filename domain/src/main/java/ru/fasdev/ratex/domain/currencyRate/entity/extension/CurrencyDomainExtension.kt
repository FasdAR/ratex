package ru.fasdev.ratex.domain.currencyRate.entity.extension

import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import java.util.*

fun Currency.toCurrencyDomain(): CurrencyDomain = CurrencyDomain(currencyCode, symbol, displayName, null)
fun Currency.toCurrencyDomain(urlImage: String?) = CurrencyDomain(currencyCode, symbol, displayName, urlImage)