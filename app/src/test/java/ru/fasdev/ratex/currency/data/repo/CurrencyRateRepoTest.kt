package ru.fasdev.ratex.currency.data.repo

import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.fasdev.ratex.currency.data.FakeRateSnapshotStorage
import ru.fasdev.ratex.currency.data.FakeRateSource
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class CurrencyRateRepoTest {
    private val snapshotV1 = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1))
    private val snapshotV2 = RateSnapshotDomain("EUR", "2026-10-06", mapOf("USD" to 1.2))

    private lateinit var source: FakeRateSource
    private lateinit var storage: FakeRateSnapshotStorage
    private var now = 10_000_000_000L

    @Before
    fun setUp() {
        source = FakeRateSource(snapshotV2)
        storage = FakeRateSnapshotStorage()
    }

    private fun createRepo() = CurrencyRateRepoImpl(source, storage) { now }

    private fun storedAgo(hours: Int, snapshot: RateSnapshotDomain = snapshotV1) =
        StoredSnapshot(snapshot, now - hours.hours.inWholeMilliseconds)

    @Test
    fun testEmptyStorageFetchesAndPersists() = runTest {
        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
        assertThat(storage.saved["fake"]).isEqualTo(StoredSnapshot(snapshotV2, now))
    }

    @Test
    fun testFreshSnapshotInStorageIsUsedWithoutNetwork() = runTest {
        // «процесс перезапущен»: память пуста, снимок лежит на диске
        storage.saved["fake"] = storedAgo(hours = 1)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV1)
        assertThat(source.fetchCount).isEqualTo(0)
    }

    @Test
    fun testStaleSnapshotInStorageIsRefetched() = runTest {
        storage.saved["fake"] = storedAgo(hours = 5)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
        assertThat(storage.saved["fake"]).isEqualTo(StoredSnapshot(snapshotV2, now))
    }

    @Test
    fun testSecondCallUsesMemory() = runTest {
        val repo = createRepo()

        repo.getSnapshot()
        repo.getSnapshot()

        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testNetworkFailureReturnsStaleSnapshotAndKeepsStorage() = runTest {
        val stale = storedAgo(hours = 30)
        storage.saved["fake"] = stale
        source.error = IOException("no network")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV1)
        assertThat(storage.saved["fake"]).isEqualTo(stale)
    }

    @Test
    fun testNetworkFailureWithoutSnapshotThrows() = runTest {
        source.error = IOException("no network")

        val error = runCatching { createRepo().getSnapshot() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
    }

    @Test
    fun testStoredSnapshotOfAnotherBaseIsIgnored() = runTest {
        storage.saved["fake"] = storedAgo(hours = 1, snapshot = RateSnapshotDomain("RUB", "2026-10-05", mapOf("USD" to 0.011)))

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testUnreadableStorageIsTreatedAsEmpty() = runTest {
        storage.loadError = IllegalStateException("database corrupted")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
    }

    @Test
    fun testFailedSaveDoesNotFailFetch() = runTest {
        storage.saveError = IOException("disk full")

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
    }

    @Test
    fun testConcurrentCallsFetchOnce() = runTest {
        source.fetchDelayMs = 1_000
        val repo = createRepo()

        val results = (1..3).map { async { repo.getSnapshot() } }.awaitAll()

        assertThat(results).allMatch { it == snapshotV2 }
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testClockMovedBackMakesSnapshotStale() = runTest {
        storage.saved["fake"] = StoredSnapshot(snapshotV1, fetchedAt = now + 1.hours.inWholeMilliseconds)

        val result = createRepo().getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testFailedFetchIsNotRetriedWithinBackoff() = runTest {
        source.error = IOException("no network")
        val repo = createRepo()

        repeat(3) { runCatching { repo.getSnapshot() } }

        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testFailedFetchWithinBackoffServesStaleSnapshot() = runTest {
        storage.saved["fake"] = storedAgo(hours = 30)
        source.error = IOException("no network")
        val repo = createRepo()

        val first = repo.getSnapshot()
        val second = repo.getSnapshot()

        assertThat(first).isEqualTo(snapshotV1)
        assertThat(second).isEqualTo(snapshotV1)
        assertThat(source.fetchCount).isEqualTo(1)
    }

    @Test
    fun testFailedFetchWithinBackoffRethrowsWithoutSnapshot() = runTest {
        source.error = IOException("no network")
        val repo = createRepo()
        runCatching { repo.getSnapshot() }

        val error = runCatching { repo.getSnapshot() }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
    }

    @Test
    fun testFetchIsRetriedAfterBackoff() = runTest {
        source.error = IOException("no network")
        val repo = createRepo()
        runCatching { repo.getSnapshot() }

        now += 60_000
        source.error = null
        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(snapshotV2)
        assertThat(source.fetchCount).isEqualTo(2)
    }
}
