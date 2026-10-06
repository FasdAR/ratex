package ru.fasdev.ratex.currency.data.storage

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class RoomRateSnapshotStorage(private val dao: RateSnapshotDao) : RateSnapshotStorage {
    override suspend fun load(sourceId: String): StoredSnapshot? {
        val (snapshot, rates) = dao.loadSnapshot(sourceId) ?: return null

        return StoredSnapshot(
            snapshot = RateSnapshotDomain(snapshot.baseCode, snapshot.date, rates.associate { it.code to it.value }),
            fetchedAt = snapshot.fetchedAt
        )
    }

    override suspend fun save(sourceId: String, snapshot: RateSnapshotDomain, fetchedAt: Long) {
        dao.replaceSnapshot(
            RateSnapshotEntity(sourceId, snapshot.baseCode, snapshot.date, fetchedAt),
            snapshot.rates.map { RateEntity(sourceId, it.key, it.value) }
        )
    }
}
