package ru.fasdev.ratex.currency.data.source.fed

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Курсы ФРС США, релиз H.10, Data Download Program: к USD, около 20 валют, ключ не нужен.
 * Релиз выходит раз в неделю (по понедельникам) и содержит дневные курсы за прошлую неделю, так что данные запаздывают до недели.
 * DDP объявлен выводимым из эксплуатации, а хэш пакета в [URL] может смениться: тогда источник начнёт падать, остальные продолжат работу.
 */
class FedRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = FedCsvParser.BASE_CODE
    override val refreshInterval: Duration = 12.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        FedCsvParser.parse(String(httpClient.get(URL).bodyAsBytes(), Charsets.UTF_8))
    }

    companion object {
        const val ID = "fed"
        const val URL = "https://www.federalreserve.gov/datadownload/Output.aspx?rel=H10&series=60f32914ab61dfab590e0e470153e3ae" +
            "&lastobs=10&from=&to=&filetype=csv&label=include&layout=seriescolumn&type=package"
    }
}
