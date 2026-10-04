package ru.fasdev.ratex.core.di.module

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.fasdev.ratex.BuildConfig
import ru.fasdev.ratex.main.di.scope.AppScope

@Module
class RetrofitModule {
    @Provides
    @AppScope
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @AppScope
    fun provideConverterFactory(json: Json): Converter.Factory = json.asConverterFactory("application/json".toMediaType())

    @Provides
    @AppScope
    fun provideHttpLogginInteractor(): HttpLoggingInterceptor {
        val httpInteractor = HttpLoggingInterceptor()

        if (BuildConfig.DEBUG) {
            httpInteractor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            httpInteractor.level = HttpLoggingInterceptor.Level.NONE
        }

        return httpInteractor
    }

    @Provides
    @AppScope
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()

    @Provides
    @AppScope
    fun provideRetrofit(okHttpClient: OkHttpClient, converterFactory: Converter.Factory): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .baseUrl("https://api.exchangeratesapi.io") // TODO: CHANGE BASE URL TO DYNAMIC
        .build()
}
