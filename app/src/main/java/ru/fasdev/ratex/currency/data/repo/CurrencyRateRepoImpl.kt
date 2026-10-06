package ru.fasdev.ratex.currency.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.fasdev.ratex.currency.data.source.CurrencyRateSource
import ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage
import ru.fasdev.ratex.currency.data.storage.StoredSnapshot
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Снимок курсов источника: память → хранилище → сеть. Свежесть считается по [CurrencyRateSource.refreshInterval].
 * Сеть упала, а снимок есть (любой давности) — отдаём его. Параллельные вызовы делят один запрос.
 * После неудачного запроса [FAILURE_BACKOFF_MS] новых запросов нет: иначе очередь вызовов офлайн ждёт таймаут сети по несколько раз подряд.
 */
class CurrencyRateRepoImpl(
    private val source: CurrencyRateSource,
    private val storage: RateSnapshotStorage,
    private val clock: () -> Long = System::currentTimeMillis
) : CurrencyRateRepo {
    private val mutex = Mutex()
    private var memory: StoredSnapshot? = null
    private var lastFailure: Failure? = null

    private class Failure(val at: Long, val error: Exception)

    override suspend fun getSnapshot(): RateSnapshotDomain = mutex.withLock {
        val cached = memory ?: loadFromStorage()?.also { memory = it }

        if (cached != null && isFresh(cached)) {
            return cached.snapshot
        }

        lastFailure?.let {
            if (clock() - it.at in 0 until FAILURE_BACKOFF_MS) return cached?.snapshot ?: throw it.error
        }

        val fetched = try {
            source.fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastFailure = Failure(clock(), e)
            return cached?.snapshot ?: throw e
        }

        lastFailure = null
        val stored = StoredSnapshot(fetched, clock())
        memory = stored
        persist(stored)

        fetched
    }

    private fun isFresh(stored: StoredSnapshot): Boolean {
        val age = clock() - stored.fetchedAt
        // age < 0 — часы переведены назад: такому снимку верить нельзя
        return age >= 0 && age < source.refreshInterval.inWholeMilliseconds
    }

    private suspend fun loadFromStorage(): StoredSnapshot? = try {
        storage.load(source.id)?.takeIf { it.snapshot.baseCode == source.baseCode }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun persist(stored: StoredSnapshot) {
        try {
            storage.save(source.id, stored.snapshot, stored.fetchedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Кэш не критичен: свежий снимок уже в памяти, в следующий раз запишем снова
        }
    }

    private companion object {
        const val FAILURE_BACKOFF_MS = 15_000L
    }
}
