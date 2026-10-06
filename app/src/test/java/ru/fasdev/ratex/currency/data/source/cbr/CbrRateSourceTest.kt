package ru.fasdev.ratex.currency.data.source.cbr

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.charset.Charset
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
class CbrRateSourceTest {
    private var status: HttpStatusCode = HttpStatusCode.OK
    private var body: ByteArray = CbrTestData.XML_DAILY.toByteArray(Charset.forName("windows-1251"))
    private var requestedUrl: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var source: CbrRateSource

    @Before
    fun setUp() {
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            // Без charset в заголовке: источник обязан декодировать windows-1251 сам
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/xml"))
        }

        httpClient = HttpClient(engine) { expectSuccess = true }
        source = CbrRateSource(httpClient, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testSourceConditions() {
        assertThat(source.id).isEqualTo("cbr")
        assertThat(source.baseCode).isEqualTo("RUB")
        assertThat(source.refreshInterval).isEqualTo(4.hours)
    }

    @Test
    fun testFetchDecodesWindows1251() = runTest {
        val result = source.fetch()

        assertThat(requestedUrl).isEqualTo("https://www.cbr.ru/scripts/XML_daily.asp")
        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.rates).containsKeys("USD", "JPY", "KZT", "BGN")
    }

    @Test
    fun testFetchHttpError() = runTest {
        status = HttpStatusCode.InternalServerError
        body = "error".toByteArray()

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ResponseException::class.java)
    }

    @Test
    fun testFetchHtmlInsteadOfXmlThrows() = runTest {
        body = CbrTestData.HTML_ERROR_PAGE.toByteArray()

        val error = runCatching { source.fetch() }.exceptionOrNull()

        assertThat(error).isNotNull()
    }
}
