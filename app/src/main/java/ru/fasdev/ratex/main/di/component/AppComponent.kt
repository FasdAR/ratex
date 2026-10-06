package ru.fasdev.ratex.main.di.component

import android.content.Context
import dagger.Component
import io.ktor.client.HttpClient
import ru.fasdev.ratex.core.di.module.HttpClientModule
import ru.fasdev.ratex.core.di.module.SettingsModule
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.currency.data.storage.CurrencyDatabase
import ru.fasdev.ratex.currency.di.module.CurrencyDatabaseModule
import ru.fasdev.ratex.main.RatexApp
import ru.fasdev.ratex.main.di.module.AppModule
import ru.fasdev.ratex.main.di.scope.AppScope

@AppScope
@Component(modules = [AppModule::class, SettingsModule::class, HttpClientModule::class, CurrencyDatabaseModule::class])
interface AppComponent {
    // Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo
    fun httpClient(): HttpClient
    fun currencyDatabase(): CurrencyDatabase

    fun inject(app: RatexApp)
}
