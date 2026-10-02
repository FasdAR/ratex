package ru.fasdev.ratex.currency.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("latest")
    suspend fun getProducts(@Query("base") baseUrl: String): ResponseBody
}
