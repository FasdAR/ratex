package ru.fasdev.ratex.currency.data.source.treasury

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Минфин США, Treasury Reporting Rates of Exchange: к USD, около 150 валют, JSON, ключ не нужен.
 * Курсы обновляются раз в квартал (на последний день квартала), так что данные могут быть старше трёх месяцев. Самый низкий приоритет.
 * Запрашиваются 250 самых свежих записей: последний квартал содержит до ~175, парсер оставляет только его.
 */
class TreasuryRateSource(private val httpClient: HttpClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    CurrencyRateSource {
    override val id: String = ID
    override val baseCode: String = TreasuryJsonParser.BASE_CODE
    override val refreshInterval: Duration = 24.hours

    override suspend fun fetch(): RateSnapshotDomain = withContext(ioDispatcher) {
        val response = httpClient.get(URL) {
            parameter("sort", "-record_date")
            parameter("page[size]", PAGE_SIZE)
            parameter("fields", "record_date,country_currency_desc,exchange_rate")
        }
        TreasuryJsonParser.parse(String(response.bodyAsBytes(), Charsets.UTF_8))
    }

    companion object {
        const val ID = "treasury"
        const val URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"
        private const val PAGE_SIZE = 250
    }
}
