package ru.fasdev.ratex.main

import android.app.Application
import ru.fasdev.ratex.core.data.sharedPrefences.SPrefences
import ru.fasdev.ratex.core.di.module.SettingsModule
import ru.fasdev.ratex.main.di.component.AppComponent
import ru.fasdev.ratex.main.di.component.DaggerAppComponent
import ru.fasdev.ratex.main.di.module.AppModule

class RatexApp : Application() {
    object DI {
        lateinit var appComponent: AppComponent
    }

    override fun onCreate() {
        super.onCreate()

        DI.appComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(applicationContext))
            .settingsModule(SettingsModule(SPrefences.NAME_SETTINGS))
            .build()
    }
}
