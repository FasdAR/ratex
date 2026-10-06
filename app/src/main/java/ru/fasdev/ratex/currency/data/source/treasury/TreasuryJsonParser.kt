package ru.fasdev.ratex.currency.data.source.treasury

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object TreasuryJsonParser {
    const val BASE_CODE = "USD"

    /**
     * Структура: `{"data":[{"record_date":"2026-09-30","country_currency_desc":"Japan-Yen","exchange_rate":"157.09"},...]}`, все значения
     * строками. Берутся только записи с самой поздней `record_date` (предыдущие кварталы содержат уже закрытые валюты). Курс — единиц валюты
     * за 1 USD. Запись с описанием не из [TreasuryCurrencyCodes] или с курсом не числом, ≤ 0, NaN/Infinity пропускается.
     * Если у нескольких стран одна валюта (CFA-франк), берётся первая запись.
     */
    fun parse(json: String): RateSnapshotDomain {
        val rows = (Json.parseToJsonElement(json).jsonObject["data"] as? JsonArray)?.map { it.jsonObject }
        check(rows != null) { "Treasury response has no data" }

        val date = rows.mapNotNull { it.string("record_date") }.maxOrNull()
        check(date != null) { "Treasury response has no date" }

        val rates = LinkedHashMap<String, Double>()
        for (row in rows) {
            if (row.string("record_date") != date) continue

            val code = row.string("country_currency_desc")?.let { TreasuryCurrencyCodes.byDescription[it] } ?: continue
            val rate = row.string("exchange_rate")?.trim()?.toDoubleOrNull() ?: continue

            if (rate > 0.0 && rate.isFinite()) rates.putIfAbsent(code, rate)
        }

        check(rates.isNotEmpty()) { "Treasury response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
