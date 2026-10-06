package ru.fasdev.ratex.currency.data.storage

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RoomRateSnapshotStorageTest {
    private lateinit var database: CurrencyDatabase
    private lateinit var storage: RateSnapshotStorage

    private val ecb = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.1, "JPY" to 160.0))

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CurrencyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = RoomRateSnapshotStorage(database.rateSnapshotDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLoadUnknownSourceReturnsNull() = runTest {
        assertThat(storage.load("ecb")).isNull()
    }

    @Test
    fun testSaveAndLoad() = runTest {
        storage.save("ecb", ecb, fetchedAt = 123L)

        assertThat(storage.load("ecb")).isEqualTo(StoredSnapshot(ecb, 123L))
    }

    @Test
    fun testSaveReplacesPreviousSnapshotOfSource() = runTest {
        storage.save("ecb", ecb, fetchedAt = 1L)
        val newer = RateSnapshotDomain("EUR", "2026-10-06", mapOf("USD" to 1.2, "PLN" to 4.3))

        storage.save("ecb", newer, fetchedAt = 2L)

        val loaded = storage.load("ecb")
        assertThat(loaded).isEqualTo(StoredSnapshot(newer, 2L))
        assertThat(loaded!!.snapshot.rates).doesNotContainKey("JPY")
    }

    @Test
    fun testSnapshotsOfDifferentSourcesAreIsolated() = runTest {
        val cbr = RateSnapshotDomain("RUB", "2026-10-06", mapOf("USD" to 0.011))

        storage.save("ecb", ecb, fetchedAt = 1L)
        storage.save("cbr", cbr, fetchedAt = 2L)

        assertThat(storage.load("ecb")).isEqualTo(StoredSnapshot(ecb, 1L))
        assertThat(storage.load("cbr")).isEqualTo(StoredSnapshot(cbr, 2L))
    }

    @Test
    fun testDeletingSnapshotCascadesToRates() = runTest {
        storage.save("ecb", ecb, fetchedAt = 1L)

        database.rateSnapshotDao().deleteSnapshot("ecb")

        assertThat(database.rateSnapshotDao().getRates("ecb")).isEmpty()
    }
}
