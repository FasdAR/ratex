package ru.fasdev.ratex.currency.data.source.ecb

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Референсные курсы ЕЦБ: к EUR, около 30 валют, обновляются в рабочие дни около 16:00 CET. Ключ не нужен.
 * ЕЦБ публикует курсы «для информации» и не рекомендует использовать их для транзакций. RUB в файле нет (приостановлен с 2022-03-01).
 */
class EcbRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = EcbXmlParser.BASE_CODE
    override val refreshInterval: Duration = 4.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        EcbXmlParser.parse(httpClient.get(URL).bodyAsText())
    }

    companion object {
        const val ID = "ecb"
        const val URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    }
}
