package ru.fasdev.ratex.currency.data.storage

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

data class StoredSnapshot(val snapshot: RateSnapshotDomain, val fetchedAt: Long)

/** Персистентное хранилище снимков курсов: по одному снимку на источник. */
interface RateSnapshotStorage {
    suspend fun load(sourceId: String): StoredSnapshot?
    suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long)
}
