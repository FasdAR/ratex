package ru.fasdev.ratex.currency.data.source.cbr

import android.util.Xml
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object CbrXmlParser {
    const val BASE_CODE = "RUB"

    private val DATE_REGEX = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")

    /**
     * Структура: `<ValCurs Date="dd.MM.yyyy"><Valute><CharCode>USD</CharCode><Nominal>1</Nominal><Value>92,5</Value></Valute>...`.
     * `Value` — рублей за `Nominal` единиц валюты, поэтому «1 RUB = Nominal / Value». Запись с кривым кодом, `Nominal` или `Value`
     * (не число, ≤ 0, NaN/Infinity) пропускается: из неё нельзя считать кросс-курс.
     */
    fun parse(xml: String): RateSnapshotDomain {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var date: String? = null
        val rates = LinkedHashMap<String, Double>()

        var code: String? = null
        var nominal: Double? = null
        var value: Double? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ValCurs" -> date = parseDate(parser.getAttributeValue(null, "Date"))
                    "Valute" -> {
                        code = null
                        nominal = null
                        value = null
                    }
                    "CharCode" -> code = parser.nextText().trim()
                    "Nominal" -> nominal = parser.nextText().trim().toDoubleOrNull()
                    "Value" -> value = parser.nextText().trim().replace(',', '.').toDoubleOrNull()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "Valute") {
                val rate = if (nominal != null && value != null && nominal > 0.0 && value > 0.0) nominal / value else null
                if (!code.isNullOrEmpty() && rate != null && rate > 0.0 && rate.isFinite()) {
                    rates[code] = rate
                }
            }
            event = parser.next()
        }

        check(date != null) { "CBR response has no date" }
        check(rates.isNotEmpty()) { "CBR response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }

    private fun parseDate(raw: String?): String? {
        val match = raw?.let { DATE_REGEX.matchEntire(it.trim()) } ?: return null
        val (day, month, year) = match.destructured
        return "$year-$month-$day"
    }
}
