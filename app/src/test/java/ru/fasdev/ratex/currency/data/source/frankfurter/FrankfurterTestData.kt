package ru.fasdev.ratex.currency.data.source.frankfurter

object FrankfurterTestData {
    /** Кусок настоящего ответа `/v2/rates?base=EUR` (2026-10-07): у ARS дата на день раньше. */
    val JSON_RATES: String = """
        [{"date":"2026-10-06","base":"EUR","quote":"ARS","rate":1710.09},
        {"date":"2026-10-07","base":"EUR","quote":"GBP","rate":0.84805},
        {"date":"2026-10-07","base":"EUR","quote":"JPY","rate":177.69},
        {"date":"2026-10-07","base":"EUR","quote":"PLN","rate":4.3752},
        {"date":"2026-10-07","base":"EUR","quote":"USD","rate":1.1239}]
    """.trimIndent()

    val JSON_WITH_BAD_RATES: String = """
        [{"date":"2026-10-07","base":"EUR","quote":"USD","rate":1.1239},
        {"date":"2026-10-07","base":"EUR","quote":"JPY","rate":0},
        {"date":"2026-10-07","base":"EUR","quote":"GBP","rate":-0.84},
        {"date":"2026-10-07","base":"EUR","quote":"EUR","rate":1.0},
        {"date":"2026-10-07","base":"EUR","quote":"PLN","rate":null}]
    """.trimIndent()

    val JSON_OTHER_BASE: String = """[{"date":"2026-10-07","base":"USD","quote":"EUR","rate":0.89}]"""

    const val JSON_EMPTY: String = "[]"

    const val HTML_ERROR_PAGE: String = "<html><body>Bad gateway</body></html>"
}
