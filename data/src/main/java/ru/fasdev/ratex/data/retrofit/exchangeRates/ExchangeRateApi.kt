package ru.fasdev.ratex.data.retrofit.exchangeRates

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi
{
    @GET("latest")
    fun getProducts(@Query("base") baseUrl: String): Call<String>
}