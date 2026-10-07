package ru.fasdev.ratex.currency.data.source

import io.ktor.client.HttpClient
import ru.fasdev.ratex.currency.data.source.frankfurter.FrankfurterRateSource

/**
 * Источники курсов в порядке приоритета: курс валюты, которая есть у нескольких источников, берётся у того, что выше.
 * Сейчас подключён только Frankfurter (базовый URL — `BuildConfig.FRANKFURTER_BASE_URL`).
 * Остальные источники (`EcbRateSource`, `CbrRateSource`, `FedRateSource`, `TreasuryRateSource`) остаются в проекте, но не подключены:
 * чтобы вернуть, добавьте константу, например `ECB({ EcbRateSource(it) })`. Чем реже источник обновляется, тем ниже его место:
 * ЕЦБ и ЦБ РФ ежедневно, ФРС раз в неделю, Treasury раз в квартал.
 */
enum class RateSourcePriority(val create: (HttpClient) -> CurrencyRateSource) {
    FRANKFURTER({ FrankfurterRateSource(it) })
}
