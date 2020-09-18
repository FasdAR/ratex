package ru.fasdev.ratex.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.app.di.scope.AppScope

@Module
class AppModule (val context: Context)
{
    @Provides
    @AppScope
    fun provideContext(): Context = context
}