package ru.fasdev.ratex.currency.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.currency.data.storage.CurrencyDatabase
import ru.fasdev.ratex.main.di.scope.AppScope

/** База в `AppComponent`: `CurrencyComponent` пересоздаётся вместе с Activity, а экземпляр Room-базы должен быть один. */
@Module
class CurrencyDatabaseModule {
    @Provides
    @AppScope
    fun provideCurrencyDatabase(context: Context): CurrencyDatabase = Room
        .databaseBuilder(context, CurrencyDatabase::class.java, CurrencyDatabase.NAME)
        .fallbackToDestructiveMigration(dropAllTables = true) // это кэш курсов, не пользовательские данные
        .build()
}
