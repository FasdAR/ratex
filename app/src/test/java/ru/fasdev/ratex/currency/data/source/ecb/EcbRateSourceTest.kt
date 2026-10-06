package ru.fasdev.ratex.currency.data.source.ecb

import android.os.Build
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EcbRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: String = EcbTestData.XML_DAILY
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var source: EcbRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/xml"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = EcbRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("ecb")
        assertThat(source.baseCode).isEqualTo("EUR")
        assertThat(source.refreshInterval).isEqualTo(4.hours)
    }

    @Test
    fun testFetch() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).isEqualTo("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml")
        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.rates).containsKeys("USD", "JPY", "GBP", "PLN")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error"

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfXmlThrows() = runTest {
        body = EcbTestData.HTML_ERROR_PAGE

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}
