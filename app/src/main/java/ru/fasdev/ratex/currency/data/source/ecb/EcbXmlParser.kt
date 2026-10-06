package ru.fasdev.ratex.currency.data.source.ecb

import android.util.Xml
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

internal object EcbXmlParser {
    const val BASE_CODE = "EUR"

    /**
     * Структура: `<Cube><Cube time="YYYY-MM-DD"><Cube currency="USD" rate="1.08"/>...`.
     * Курсы не больше нуля, не числа и NaN/Infinity пропускаются: из них нельзя считать кросс-курс.
     */
    fun parse(xml: String): RateSnapshotDomain {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var date: String? = null
        val rates = LinkedHashMap<String, Double>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Cube") {
                val time = parser.getAttributeValue(null, "time")
                val currency = parser.getAttributeValue(null, "currency")
                val rate = parser.getAttributeValue(null, "rate")?.toDoubleOrNull()

                if (time != null) {
                    date = time
                } else if (currency != null && rate != null && rate > 0.0 && rate.isFinite()) {
                    rates[currency] = rate
                }
            }
            event = parser.next()
        }

        check(date != null) { "ECB response has no date" }
        check(rates.isNotEmpty()) { "ECB response has no rates" }

        return RateSnapshotDomain(BASE_CODE, date, rates)
    }
}
