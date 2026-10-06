package ru.fasdev.ratex.currency.data.source.treasury

object TreasuryTestData {
    private fun row(date: String, desc: String, rate: String?): String {
        val value = if (rate == null) "null" else "\"$rate\""
        return """{"record_date":"$date","country_currency_desc":"$desc","exchange_rate":$value,"effective_date":"$date"}"""
    }

    private fun response(vararg rows: String) = """{"data":[${rows.joinToString(",")}],"meta":{"count":${rows.size}}}"""

    /** Строки 2026-09-30 настоящие, строка за 2026-06-30 осталась от квартала, в котором Болгария ещё была не в еврозоне. */
    val JSON_DAILY: String = response(
        row("2026-09-30", "Euro Zone-Euro", "0.881"),
        row("2026-09-30", "Japan-Yen", "157.09"),
        row("2026-09-30", "Australia-Dollar", "1.435"),
        row("2026-09-30", "Zimbabwe-Gold", "25.337"),
        row("2026-06-30", "Bulgaria-Lev New", "1.72")
    )

    /** CFA-франк: у одной валюты несколько стран. */
    val JSON_SHARED_CURRENCY: String = response(
        row("2026-09-30", "Benin-Cfa Franc", "573.75"),
        row("2026-09-30", "Senegal-Cfa Franc", "573.75"),
        row("2026-09-30", "Cameroon-Cfa Franc", "577.72"),
        row("2026-09-30", "Chad-Cfa Franc", "577.72")
    )

    /** Привязка к доллару (1.0): USD в снимок не попадает. */
    val JSON_USD_PEGGED: String = response(
        row("2026-09-30", "Panama-Dolares", "1.0"),
        row("2026-09-30", "Ecuador-Dolares", "1.0"),
        row("2026-09-30", "Bahamas-Dollar", "1.0"),
        row("2026-09-30", "Japan-Yen", "157.09")
    )

    /** Записи Treasury, у которых курс заведомо неверный (при сверке с ФРС совпадает с курсом другой валюты). */
    val JSON_KNOWN_BAD_ROWS: String = response(
        row("2026-09-30", "Nepal-Rupee", "1.771"),
        row("2026-09-30", "New Zealand-Dollar", "0.881"),
        row("2026-09-30", "Curacao-Caribbean Guilder", "0.881"),
        row("2026-09-30", "Japan-Yen", "157.09")
    )

    val JSON_WITH_BAD_VALUES: String = response(
        row("2026-09-30", "Japan-Yen", "157.09"),
        row("2026-09-30", "Australia-Dollar", null),
        row("2026-09-30", "Euro Zone-Euro", ""),
        row("2026-09-30", "Canada-Dollar", "abc"),
        row("2026-09-30", "Denmark-Krone", "0"),
        row("2026-09-30", "Norway-Krone", "-9.59"),
        row("2026-09-30", "Unknown Land-Coin", "5.0")
    )

    val JSON_NO_ROWS: String = response()

    val JSON_ONLY_UNKNOWN: String = response(row("2026-09-30", "Unknown Land-Coin", "5.0"))

    val JSON_NO_DATA_FIELD: String = """{"error":"Bad Request","message":"Invalid query"}"""

    const val HTML_ERROR_PAGE: String = "<html><body><h1>503 Service Unavailable</h1></body></html>"
}
