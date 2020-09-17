package ru.fasdev.ratex.app.di.module

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.app.di.scope.AppScope
import ru.fasdev.ratex.data.sharedPrefences.SPrefences
import ru.fasdev.ratex.data.sharedPrefences.SharedPrefencesRepoImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import javax.inject.Inject
import javax.inject.Named

@Module
class SettingsModule ()
{
    @set:[Inject Named("name_settings")] var nameSettings: String = ""

    @Provides
    @AppScope
    fun provideSharedPrefences(context: Context): SharedPreferences = context.getSharedPreferences(nameSettings, Context.MODE_PRIVATE)

    @Provides
    @AppScope
    fun provideSPrefences(sharedPreferences: SharedPreferences): SPrefences = SPrefences(sharedPreferences)

    @Provides
    @AppScope
    fun provideSharedPrefencesRepo(sPrefences: SPrefences): SharedPrefencesRepo = SharedPrefencesRepoImpl(sPrefences)
}