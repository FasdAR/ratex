package ru.fasdev.ratex.currency.data.api

import ru.fasdev.ratex.currency.data.api.model.ExchangeRatesResponse

interface ExchangeRateApi {
    suspend fun getProducts(baseUrl: String): ExchangeRatesResponse
}
