package ru.fasdev.ratex.app.di.module.network

import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.fasdev.ratex.BuildConfig
import ru.fasdev.ratex.app.di.scope.ActivityScope
import ru.fasdev.ratex.app.di.scope.AppScope

@Module
class RetrofitModule
{
    @Provides
    @AppScope
    fun provideRxAdapter(): RxJava2CallAdapterFactory = RxJava2CallAdapterFactory.createWithScheduler(
        Schedulers.io())

    @Provides
    @AppScope
    fun provideGsonConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()


    @Provides
    @AppScope
    fun provideHttpLogginInteractor(): HttpLoggingInterceptor {
        val httpInteractor = HttpLoggingInterceptor()

        if (BuildConfig.DEBUG)
            httpInteractor.level = HttpLoggingInterceptor.Level.BODY
        else
            httpInteractor.level = HttpLoggingInterceptor.Level.NONE

        return httpInteractor
    }

    @Provides
    @AppScope
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()

    @Provides
    @AppScope
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gsonConverterFactory: GsonConverterFactory,
        callAdapterFactory: RxJava2CallAdapterFactory
    ): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(callAdapterFactory)
        .baseUrl("https://api.exchangeratesapi.io") //TODO: CHANGE BASE URL TO DYNAMIC
        .build()
}