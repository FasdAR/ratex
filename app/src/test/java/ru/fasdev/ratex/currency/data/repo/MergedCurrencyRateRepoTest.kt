package ru.fasdev.ratex.currency.data.repo

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import ru.fasdev.ratex.currency.data.source.RateSourcePriority
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

class MergedCurrencyRateRepoTest {
    private class FakeRateRepo(var snapshot: RateSnapshotDomain? = null, var error: Exception? = null, var delayMs: Long = 0L) :
        CurrencyRateRepo {
        override suspend fun getSnapshot(): RateSnapshotDomain {
            if (delayMs > 0) delay(delayMs)
            error?.let { throw it }
            return snapshot!!
        }
    }

    private val ecb = RateSnapshotDomain("EUR", "2026-10-05", mapOf("USD" to 1.10, "JPY" to 160.0, "CNY" to 7.8))

    // USD и CNY есть у обоих источников, BGN и KZT только у ЦБ РФ
    private val cbr = RateSnapshotDomain(
        "RUB",
        "2026-10-06",
        mapOf("USD" to 1 / 90.0, "CNY" to 1 / 12.0, "BGN" to 1 / 50.0, "KZT" to 100 / 18.9)
    )

    private val offset = Offset.offset(1e-9)

    @Test
    fun testBridgeAddsMissingCurrenciesInPrimaryScale() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.availableCodes).contains("EUR", "USD", "JPY", "CNY", "RUB", "BGN", "KZT")
        // мост по USD: 1 EUR = 1.10 USD = 1.10 * 90 RUB
        assertThat(result.rates.getValue("RUB")).isCloseTo(1.10 * 90.0, offset)
        assertThat(result.rates.getValue("BGN")).isCloseTo(1.10 * 90.0 / 50.0, offset)
        assertThat(result.rates.getValue("KZT")).isCloseTo(1.10 * 90.0 * 100 / 18.9, offset)
    }

    @Test
    fun testPrimaryRatesAreNotOverwritten() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result.rates.getValue("USD")).isEqualTo(1.10)
        assertThat(result.rates.getValue("CNY")).isEqualTo(7.8)
        assertThat(result.rates).doesNotContainKey("EUR")
    }

    @Test
    fun testDateIsTheLatest() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr)))

        assertThat(repo.getSnapshot().date).isEqualTo("2026-10-06")
    }

    @Test
    fun testUsdBaseSourcesOnlyAddMissingCurrenciesInPriorityOrder() = runTest {
        // JPY есть у всех, VES у ФРС и Treasury, KES только у Treasury
        val fed = RateSnapshotDomain("USD", "2026-10-02", mapOf("JPY" to 150.0, "VES" to 800.0))
        val treasury = RateSnapshotDomain("USD", "2026-09-30", mapOf("JPY" to 140.0, "VES" to 600.0, "KES" to 129.0))
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr), FakeRateRepo(fed), FakeRateRepo(treasury)))

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.rates.getValue("JPY")).isEqualTo(160.0)
        // 1 EUR = 1.10 USD: курсы к USD пересчитываются в шкалу ЕЦБ
        assertThat(result.rates.getValue("VES")).isCloseTo(800.0 * 1.10, offset)
        assertThat(result.rates.getValue("KES")).isCloseTo(129.0 * 1.10, offset)
        assertThat(result.date).isEqualTo("2026-10-06")
    }

    @Test
    fun testOrderIsPriority() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(cbr), FakeRateRepo(ecb)))

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("RUB")
        assertThat(result.rates.getValue("USD")).isEqualTo(1 / 90.0)
        assertThat(result.availableCodes).contains("EUR", "JPY")
        assertThat(result.rates.getValue("EUR")).isCloseTo(1 / 90.0 / 1.10, offset)
    }

    @Test
    fun testBridgeFallsBackToAnyCommonCurrencyWhenNoUsd() = runTest {
        val first = RateSnapshotDomain("EUR", "2026-10-05", mapOf("CNY" to 8.0))
        val second = RateSnapshotDomain("RUB", "2026-10-05", mapOf("CNY" to 1 / 10.0, "BGN" to 1 / 50.0))
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(first), FakeRateRepo(second)))

        val result = repo.getSnapshot()

        // 1 EUR = 8 CNY = 80 RUB
        assertThat(result.rates.getValue("RUB")).isCloseTo(80.0, offset)
        assertThat(result.rates.getValue("BGN")).isCloseTo(80.0 / 50.0, offset)
    }

    @Test
    fun testSnapshotWithoutCommonCurrencyIsSkipped() = runTest {
        val isolated = RateSnapshotDomain("RUB", "2026-10-06", mapOf("BGN" to 1 / 50.0))
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(isolated)))

        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(ecb)
    }

    @Test
    fun testOneSourceFailsUsesTheOther() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(error = IOException("offline")), FakeRateRepo(cbr)))

        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(cbr)
    }

    @Test
    fun testSecondarySourceFailsKeepsPrimary() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(error = IOException("offline"))))

        assertThat(repo.getSnapshot()).isEqualTo(ecb)
    }

    @Test
    fun testSlowSecondarySourceDoesNotDelayPrimary() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(cbr, delayMs = 60_000)), secondaryTimeout = 3.seconds)

        val result = repo.getSnapshot()

        assertThat(result).isEqualTo(ecb)
        assertThat(testScheduler.currentTime).isLessThan(60_000)
    }

    @Test
    fun testSlowPrimarySourceIsNotCutOff() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb, delayMs = 10_000), FakeRateRepo(cbr)), secondaryTimeout = 3.seconds)

        val result = repo.getSnapshot()

        assertThat(result.baseCode).isEqualTo("EUR")
        assertThat(result.availableCodes).contains("RUB")
    }

    @Test
    fun testAllSourcesFailThrowsFirstError() = runTest {
        val first = IOException("first")
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(error = first), FakeRateRepo(error = IOException("second"))))

        val error = runCatching { repo.getSnapshot() }.exceptionOrNull()

        assertThat(error).isSameAs(first)
    }

    @Test
    fun testCancellationIsRethrown() = runTest {
        val repo = MergedCurrencyRateRepo(listOf(FakeRateRepo(ecb), FakeRateRepo(error = CancellationException("cancelled"))))

        val error = runCatching { repo.getSnapshot() }.exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun testPriorityEnumOrderIsDailySourcesThenWeeklyThenQuarterly() {
        assertThat(RateSourcePriority.entries).containsExactly(
            RateSourcePriority.ECB,
            RateSourcePriority.CBR,
            RateSourcePriority.FED,
            RateSourcePriority.TREASURY
        )
    }
}
