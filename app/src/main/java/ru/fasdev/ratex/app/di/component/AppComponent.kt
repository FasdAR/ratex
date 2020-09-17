package ru.fasdev.ratex.app.di.component

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.module.SettingsModule
import javax.inject.Named

@Component(modules = [SettingsModule::class])
interface AppComponent
{
    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        @Named("name_settings")
        fun nameSettings(name: String): Builder
    }

    fun inject(app: RatexApp)
}