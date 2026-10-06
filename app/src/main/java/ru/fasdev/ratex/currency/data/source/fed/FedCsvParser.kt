package ru.fasdev.ratex.currency.data.source.fed

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object FedCsvParser {
    const val BASE_CODE = "USD"

    private val DATA_ROW_REGEX = Regex("""\d{4}-\d{2}-\d{2},.*""")

    // ФРС оставила у боливара старый код: в строке `Currency:` стоит VEB, в идентификаторе серии и в ISO — VES
    private val CODE_ALIASES = mapOf("VEB" to "VES")

    /**
     * Структура DDP-пакета H.10 (CSV): строка `"Currency:"` с ISO-кодами, строка `"Unique Identifier:"` и строки `дата,значения…`.
     * Направление котировки берётся из идентификатора серии: `RXI$US_*` — USD за единицу валюты (AUD, EUR, GBP, NZD), `RXI_*` — единиц
     * валюты за USD. Для каждой валюты берётся последнее число, `ND` и не числа (в том числе ≤ 0, NaN/Infinity) пропускаются.
     * Дата снимка — самая поздняя дата, из которой взят хотя бы один курс.
     */
    fun parse(csv: String): RateSnapshotDomain {
        val lines = csv.lines().map { it.trim() }

        val rows = lines.filter { it.startsWith('"') }.map(::splitRow)
        // Подпись строки в разных выгрузках бывает с пробелом внутри кавычек: "Unique Identifier: "
        val codes = rows.firstOrNull { it.first() == "Currency:" }
        val seriesIds = rows.firstOrNull { it.first() == "Unique Identifier:" }
        check(codes != null && seriesIds != null) { "Fed response has no currency header" }

        val dataRows = lines.filter { DATA_ROW_REGEX.matches(it) }.map(::splitRow).sortedBy { it.first() }

        val rates = LinkedHashMap<String, Double>()
        var date: String? = null

        // В строках `Currency:` и `Unique Identifier:` нулевая ячейка — подпись строки, у данных — дата
        for (column in 1 until minOf(codes.size, seriesIds.size)) {
            val code = codes[column].let { CODE_ALIASES[it] ?: it }
            val last = dataRows.lastOrNull { parseValue(it.getOrNull(column)) != null } ?: continue
            val value = parseValue(last[column]) ?: continue

            val perUsd = if (seriesIds[column].contains("RXI\$US_")) 1 / value else value
            if (code.isNotEmpty() && perUsd > 0.0 && perUsd.isFinite()) {
                rates[code] = perUsd
                if (date == null || last.first() > date) date = last.first()
            }
        }

        check(date != null && rates.isNotEmpty()) { "Fed response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }

    private fun splitRow(line: String): List<String> = line.split(',').map { it.trim().trim('"').trim() }

    private fun parseValue(raw: String?): Double? = raw?.toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
}
