package ru.fasdev.ratex.currency.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RateSnapshotEntity::class, RateEntity::class], version = 1, exportSchema = true)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun rateSnapshotDao(): RateSnapshotDao

    companion object {
        const val NAME = "ratex.db"
    }
}
