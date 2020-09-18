package ru.fasdev.ratex.app.di.module.app

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.app.di.scope.AppScope
import ru.fasdev.ratex.data.sharedPrefences.SPrefences
import ru.fasdev.ratex.data.sharedPrefences.SharedPrefencesRepoImpl
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo

@Module
class SettingsModule (val nameSettings: String)
{
    @Provides
    @AppScope
    fun provideSharedPreneces(context: Context): SharedPreferences = context.getSharedPreferences(nameSettings, Context.MODE_PRIVATE)

    @Provides
    @AppScope
    fun provideSPrefences(sharedPreferences: SharedPreferences): SPrefences = SPrefences(sharedPreferences)

    @Provides
    @AppScope
    fun provideSharedPrefencesRepo(sPrefences: SPrefences): SharedPrefencesRepo = SharedPrefencesRepoImpl(sPrefences)
}