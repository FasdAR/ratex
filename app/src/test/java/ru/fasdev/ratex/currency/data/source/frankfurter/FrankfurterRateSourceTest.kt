package ru.fasdev.ratex.currency.data.source.frankfurter

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

class FrankfurterRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: String = FrankfurterTestData.JSON_RATES
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    private fun createSource(baseUrl: String = "https://api.frankfurter.dev") =
        FrankfurterRateSource(httpClient, baseUrl, UnconfinedTestDispatcher())

    @Test
    fun testSourceConditions() {
        val source = createSource()

        assertThat(source.id).isEqualTo("frankfurter")
        assertThat(source.baseCode).isEqualTo("EUR")
        assertThat(source.refreshInterval).isEqualTo(20.hours)
    }

    @Test
    fun testFetch() = runTest {
        val result = createSource().fetch()

        assertThat(requestedUrl).isEqualTo("https://api.frankfurter.dev/v2/rates?base=EUR")
        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.rates).containsKeys("USD", "JPY", "GBP", "PLN")
    }

    @Test
    fun testBaseUrlIsConfigurable() = runTest {
        createSource("https://frankfurter.example.test:8080/").fetch()

        assertThat(requestedUrl).isEqualTo("https://frankfurter.example.test:8080/v2/rates?base=EUR")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error"

        val error = runCatching { createSource().fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfJsonThrows() = runTest {
        body = FrankfurterTestData.HTML_ERROR_PAGE

        val error = runCatching { createSource().fetch() }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
