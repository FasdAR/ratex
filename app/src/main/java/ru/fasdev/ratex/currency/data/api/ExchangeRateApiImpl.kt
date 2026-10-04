package ru.fasdev.ratex.currency.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.fasdev.ratex.currency.data.api.model.ExchangeRatesResponse

class ExchangeRateApiImpl(private val httpClient: HttpClient) : ExchangeRateApi {
    override suspend fun getProducts(baseUrl: String): ExchangeRatesResponse = httpClient
        .get("latest") {
            parameter("base", baseUrl)
        }
        .body()
}
