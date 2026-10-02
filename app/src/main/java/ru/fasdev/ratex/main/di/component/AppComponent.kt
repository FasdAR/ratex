package ru.fasdev.ratex.main.di.component

import android.content.Context
import dagger.Component
import retrofit2.Retrofit
import ru.fasdev.ratex.core.di.module.RetrofitModule
import ru.fasdev.ratex.core.di.module.SettingsModule
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.main.RatexApp
import ru.fasdev.ratex.main.di.module.AppModule
import ru.fasdev.ratex.main.di.scope.AppScope

@AppScope
@Component(modules = [AppModule::class, SettingsModule::class, RetrofitModule::class])
interface AppComponent {
    // Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo
    fun retrofit(): Retrofit

    fun inject(app: RatexApp)
}
