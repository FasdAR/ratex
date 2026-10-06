package ru.fasdev.ratex.currency.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.fasdev.ratex.currency.domain.boundaries.repo.CurrencyRateRepo
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Один снимок из нескольких источников. [repos] — по убыванию приоритета: шкалой (базой) снимка становится база первого
 * успешного источника, курсы остальных пересчитываются в неё через общую валюту (мост). Валюта, которая уже есть у более
 * приоритетного источника, не перезаписывается. Источник без общей валюты пропускается, упавший — тоже, если жив хоть один.
 */
class MergedCurrencyRateRepo(private val repos: List<CurrencyRateRepo>) : CurrencyRateRepo {
    override suspend fun getSnapshot(): RateSnapshotDomain {
        val results = coroutineScope {
            repos.map { repo ->
                async {
                    try {
                        Result.success(repo.getSnapshot())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }

        val snapshots = results.mapNotNull { it.getOrNull() }
        if (snapshots.isEmpty()) {
            throw results.firstNotNullOfOrNull { it.exceptionOrNull() } ?: IllegalStateException("No rate sources")
        }

        return snapshots.reduce(::merge)
    }

    private fun merge(primary: RateSnapshotDomain, other: RateSnapshotDomain): RateSnapshotDomain {
        val merged = primary.rates + (primary.baseCode to 1.0)
        val otherAll = other.rates + (other.baseCode to 1.0)

        val bridge = if (BRIDGE_CODE in merged && BRIDGE_CODE in otherAll) {
            BRIDGE_CODE
        } else {
            otherAll.keys.firstOrNull { it in merged }
        } ?: return primary

        val scale = merged.getValue(bridge) / otherAll.getValue(bridge)

        val result = LinkedHashMap(merged)
        otherAll.forEach { (code, rate) ->
            if (code !in result) result[code] = rate * scale
        }
        result.remove(primary.baseCode)

        return RateSnapshotDomain(primary.baseCode, maxOf(primary.date, other.date), result)
    }

    private companion object {
        const val BRIDGE_CODE = "USD"
    }
}
