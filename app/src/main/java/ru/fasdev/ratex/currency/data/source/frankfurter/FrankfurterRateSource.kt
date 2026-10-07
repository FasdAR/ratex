package ru.fasdev.ratex.currency.data.source.frankfurter

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.BuildConfig
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Frankfurter API: курсы около 160 валют от центробанков, к EUR, ключ и квоты не нужны.
 * Адрес сервера ([baseUrl], без версии API) задаётся в сборке: `BuildConfig.FRANKFURTER_BASE_URL` (см. `app/build.gradle.kts`).
 * Сервер отдаёт ответ с `cache-control: max-age=71640` (около 19,9 часа): запрос чаще не даёт новых данных, поэтому
 * [refreshInterval] — 20 часов. Дата у разных валют разная (выходные, праздники центробанков).
 */
class FrankfurterRateSource(
    private val httpClient: HttpClient,
    baseUrl: String = BuildConfig.FRANKFURTER_BASE_URL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CurrencyRateSource {
    private val ratesUrl = "${baseUrl.trimEnd('/')}$RATES_PATH"

    override val id: String = ID
    override val baseCode: String = FrankfurterJsonParser.BASE_CODE
    override val refreshInterval: Duration = 20.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        FrankfurterJsonParser.parse(
            httpClient.get(ratesUrl) { parameter("base", baseCode) }.bodyAsText()
        )
    }

    companion object {
        const val ID = "frankfurter"
        const val RATES_PATH = "/v2/rates"
    }
}
