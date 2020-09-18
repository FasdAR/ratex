package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.module.AppModule
import ru.fasdev.ratex.app.di.module.SettingsModule

@Component(modules = [AppModule::class, SettingsModule::class])
interface AppComponent
{
    fun inject(app: RatexApp)
}