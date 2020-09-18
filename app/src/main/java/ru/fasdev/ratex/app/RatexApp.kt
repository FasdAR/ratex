package ru.fasdev.ratex.app

import android.app.Application
import ru.fasdev.ratex.app.di.component.AppComponent
import ru.fasdev.ratex.app.di.component.DaggerAppComponent
import ru.fasdev.ratex.app.di.module.app.AppModule
import ru.fasdev.ratex.app.di.module.app.SettingsModule
import ru.fasdev.ratex.data.sharedPrefences.SPrefences

class RatexApp : Application()
{
    object DI
    {
        lateinit var appComponent: AppComponent
    }

    override fun onCreate()
    {
        super.onCreate()

        DI.appComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(applicationContext))
            .settingsModule(SettingsModule(SPrefences.NAME_SETTINGS))
            .build()
    }
}