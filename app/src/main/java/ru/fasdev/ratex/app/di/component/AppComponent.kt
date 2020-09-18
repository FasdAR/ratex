package ru.fasdev.ratex.app.di.component

import android.content.Context
import dagger.Component
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.module.app.AppModule
import ru.fasdev.ratex.app.di.module.app.SettingsModule
import ru.fasdev.ratex.app.di.scope.AppScope
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@AppScope
@Component(modules = [AppModule::class, SettingsModule::class])
interface AppComponent
{
    //Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo

    fun inject(app: RatexApp)
}