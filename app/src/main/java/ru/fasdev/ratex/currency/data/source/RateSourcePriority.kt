package ru.fasdev.ratex.currency.data.source

import io.ktor.client.HttpClient
import ru.fasdev.ratex.currency.data.source.cbr.CbrRateSource
import ru.fasdev.ratex.currency.data.source.ecb.EcbRateSource

/**
 * Источники курсов в порядке приоритета: курс валюты, которая есть у нескольких источников, берётся у того, что выше.
 * Чтобы поменять приоритет, переставьте константы.
 */
enum class RateSourcePriority(val create: (HttpClient) -> CurrencyRateSource) {
    ECB({ EcbRateSource(it) }),
    CBR({ CbrRateSource(it) })
}
