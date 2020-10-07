package ru.fasdev.ratex.data.source.retrofit.exchangeRates

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi
{
    @GET("latest")
    fun getProducts(@Query("base") baseUrl: String): Single<ResponseBody>
}