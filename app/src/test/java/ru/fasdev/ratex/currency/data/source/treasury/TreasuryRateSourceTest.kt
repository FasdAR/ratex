package ru.fasdev.ratex.currency.data.source.treasury

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class TreasuryRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: String = TreasuryTestData.JSON_DAILY
    private var requestedUrl: String? = null
    private var requestedParameters: Parameters = Parameters.Empty

    private lateinit var httpClient: HttpClient
    private lateinit var source: TreasuryRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            requestedParameters = request.url.parameters
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = TreasuryRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("treasury")
        assertThat(source.baseCode).isEqualTo("USD")
        assertThat(source.refreshInterval).isEqualTo(24.hours)
    }

    @Test
    fun testFetchRequestsLatestRecordsFirst() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).startsWith("${TreasuryRateSource.URL}?")
        assertThat(TreasuryRateSource.URL).isEqualTo(EXPECTED_URL)
        assertThat(requestedParameters["sort"]).isEqualTo("-record_date")
        assertThat(requestedParameters["page[size]"]).isEqualTo("250")
        assertThat(requestedParameters["fields"]).isEqualTo("record_date,country_currency_desc,exchange_rate")
        assertThat(result.baseCode).isEqualTo("USD")
        assertThat(result.rates).containsKeys("EUR", "JPY")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error"

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfJsonThrows() = runTest {
        body = TreasuryTestData.HTML_ERROR_PAGE

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isNotNull()
    }

    private companion object {
        const val EXPECTED_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"
    }
}
