package ru.fasdev.ratex.currency.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RateSnapshotDao {
    @Query("SELECT * FROM rate_snapshot WHERE sourceId = :sourceId")
    abstract suspend fun getSnapshot(sourceId: String): RateSnapshotEntity?

    @Query("SELECT * FROM rate WHERE sourceId = :sourceId")
    abstract suspend fun getRates(sourceId: String): List<RateEntity>

    @Insert
    abstract suspend fun insertSnapshot(snapshot: RateSnapshotEntity)

    @Insert
    abstract suspend fun insertRates(rates: List<RateEntity>)

    // Строки rate удаляются каскадно
    @Query("DELETE FROM rate_snapshot WHERE sourceId = :sourceId")
    abstract suspend fun deleteSnapshot(sourceId: String)

    @Transaction
    open suspend fun replaceSnapshot(snapshot: RateSnapshotEntity, rates: List<RateEntity>) {
        deleteSnapshot(snapshot.sourceId)
        insertSnapshot(snapshot)
        insertRates(rates)
    }

    @Transaction
    open suspend fun loadSnapshot(sourceId: String): Pair<RateSnapshotEntity, List<RateEntity>>? {
        val snapshot = getSnapshot(sourceId) ?: return null
        return snapshot to getRates(sourceId)
    }
}
