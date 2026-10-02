package ru.fasdev.ratex.core.di.module

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.core.data.sharedPrefences.SPrefences
import ru.fasdev.ratex.core.data.sharedPrefences.SharedPrefencesRepoImpl
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.main.di.scope.AppScope

@Module
class SettingsModule(val nameSettings: String) {
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
