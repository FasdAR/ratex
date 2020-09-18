package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.module.app.AppModule
import ru.fasdev.ratex.app.di.module.app.SettingsModule
import ru.fasdev.ratex.app.di.scope.AppScope

@AppScope
@Component(modules = [AppModule::class, SettingsModule::class])
interface AppComponent
{
    fun inject(app: RatexApp)
}