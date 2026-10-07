package ru.fasdev.ratex.currency.data.source.frankfurter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object FrankfurterJsonParser {
    const val BASE_CODE = "EUR"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class Record(val date: String, val base: String, val quote: String, val rate: Double? = null)

    /**
     * Структура: `[{"date":"2026-10-07","base":"EUR","quote":"USD","rate":1.12},...]`.
     * У каждой валюты своя дата публикации, датой снимка считается самая поздняя.
     * Курсы не больше нуля, NaN/Infinity, `null` и сама база пропускаются: из них нельзя считать кросс-курс.
     */
    fun parse(text: String): RateSnapshotDomain {
        val records = json.decodeFromString<List<Record>>(text)
        check(records.all { it.base == BASE_CODE }) { "Frankfurter response has base other than $BASE_CODE" }

        val rates = LinkedHashMap<String, Double>()
        var date: String? = null

        records.forEach {
            val rate = it.rate
            if (it.quote != BASE_CODE && rate != null && rate > 0.0 && rate.isFinite()) {
                rates[it.quote] = rate
                if (date == null || it.date > date!!) date = it.date
            }
        }

        check(date != null && rates.isNotEmpty()) { "Frankfurter response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date!!, rates)
    }
}
