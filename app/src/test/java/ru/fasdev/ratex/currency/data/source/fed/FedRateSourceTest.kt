package ru.fasdev.ratex.currency.data.source.fed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class FedRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: String = FedTestData.CSV_DAILY
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var source: FedRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/csv"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = FedRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("fed")
        assertThat(source.baseCode).isEqualTo("USD")
        assertThat(source.refreshInterval).isEqualTo(12.hours)
    }

    @Test
    fun testFetchRequestsCurrencyPackage() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).startsWith("https://www.federalreserve.gov/datadownload/Output.aspx?")
        assertThat(requestedUrl).contains("rel=H10", "series=60f32914ab61dfab590e0e470153e3ae", "filetype=csv", "type=package")
        assertThat(result.baseCode).isEqualTo("USD")
        assertThat(result.rates).containsKeys("EUR", "JPY", "VES")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error"

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfCsvThrows() = runTest {
        body = FedTestData.HTML_ERROR_PAGE

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}
