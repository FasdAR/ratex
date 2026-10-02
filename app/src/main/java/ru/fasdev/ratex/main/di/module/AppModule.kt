package ru.fasdev.ratex.main.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.main.di.scope.AppScope

@Module
class AppModule(val context: Context) {
    @Provides
    @AppScope
    fun provideContext(): Context = context
}
