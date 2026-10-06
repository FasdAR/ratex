package ru.fasdev.ratex.currency.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class FakeRateSource(
    var snapshot: RateSnapshotDomain,
    override val id: String = "fake",
    override val baseCode: String = snapshot.baseCode,
    override val refreshInterval: Duration = 4.hours
) : CurrencyRateSource {
    var fetchCount = 0
    var error: Exception? = null
    var fetchDelayMs = 0L

    override suspend fun fetch(): RateSnapshotDomain {
        fetchCount++
        if (fetchDelayMs > 0) delay(fetchDelayMs)
        error?.let { throw it }
        return snapshot
    }
}

class FakeRateSnapshotStorage : RateSnapshotStorage {
    val saved = mutableMapOf<String, StoredSnapshot>()
    var loadError: Exception? = null
    var saveError: Exception? = null

    override suspend fun load(sourceId: String): StoredSnapshot? {
        loadError?.let { throw it }
        return saved[sourceId]
    }

    override suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long) {
        saveError?.let { throw it }
        saved[sourceId] = StoredSnapshot(snapshot, fetchedAt)
    }
}
