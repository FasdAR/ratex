package ru.fasdev.ratex.currency.data.source.cbr

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Официальные курсы ЦБ РФ: к RUB, около 55 валют, ответ в windows-1251. Ключ не нужен.
 * Курсы на следующий день публикуются вечером, поэтому в течение дня файл не меняется.
 */
class CbrRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = CbrXmlParser.BASE_CODE
    override val refreshInterval: Duration = 4.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        // Кодировку читаем из файла сами: charset в заголовке ответа ненадёжен
        val xml = String(httpClient.get(URL).bodyAsBytes(), Charset.forName("windows-1251"))
        CbrXmlParser.parse(xml)
    }

    companion object {
        const val ID = "cbr"
        const val URL = "https://www.cbr.ru/scripts/XML_daily.asp"
    }
}
