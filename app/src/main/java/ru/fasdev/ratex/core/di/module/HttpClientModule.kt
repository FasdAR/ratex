package ru.fasdev.ratex.core.di.module

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.fasdev.ratex.BuildConfig
import ru.fasdev.ratex.main.di.scope.AppScope

@Module
class HttpClientModule {
    @Provides
    @AppScope
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @AppScope
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json, contentType = ContentType.Any) // как Retrofit-конвертер: не проверяем Content-Type ответа
        }

        install(Logging) {
            logger = Logger.ANDROID
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }

        defaultRequest {
            url("https://api.exchangeratesapi.io/") // TODO: CHANGE BASE URL TO DYNAMIC
        }
    }
}
