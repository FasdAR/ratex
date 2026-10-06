package ru.fasdev.ratex.currency.data.source

import io.ktor.client.HttpClient
import ru.fasdev.ratex.currency.data.source.cbr.CbrRateSource
import ru.fasdev.ratex.currency.data.source.ecb.EcbRateSource
import ru.fasdev.ratex.currency.data.source.fed.FedRateSource
import ru.fasdev.ratex.currency.data.source.treasury.TreasuryRateSource

/**
 * Источники курсов в порядке приоритета: курс валюты, которая есть у нескольких источников, берётся у того, что выше.
 * Чем реже источник обновляется, тем ниже он стоит: ЕЦБ и ЦБ РФ ежедневно, ФРС раз в неделю, Treasury раз в квартал.
 * Чтобы поменять приоритет, переставьте константы.
 */
enum class RateSourcePriority(val create: (HttpClient) -> CurrencyRateSource) {
    ECB({ EcbRateSource(it) }),
    CBR({ CbrRateSource(it) }),
    FED({ FedRateSource(it) }),
    TREASURY({ TreasuryRateSource(it) })
}
