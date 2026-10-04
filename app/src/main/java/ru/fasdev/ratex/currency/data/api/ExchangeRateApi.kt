package ru.fasdev.ratex.currency.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import ru.fasdev.ratex.currency.data.api.model.ExchangeRatesResponse

interface ExchangeRateApi {
    @GET("latest")
    suspend fun getProducts(@Query("base") baseUrl: String): ExchangeRatesResponse
}
