package ru.fasdev.ratex.app.di.component

import android.content.Context
import dagger.Component
import retrofit2.Retrofit
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.module.app.AppModule
import ru.fasdev.ratex.app.di.module.app.SettingsModule
import ru.fasdev.ratex.app.di.module.network.RetrofitModule
import ru.fasdev.ratex.app.di.scope.AppScope
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@AppScope
@Component(modules = [AppModule::class, SettingsModule::class, RetrofitModule::class])
interface AppComponent
{
    //Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo
    fun retrofit(): Retrofit

    fun inject(app: RatexApp)
}