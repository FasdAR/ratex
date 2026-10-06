package ru.fasdev.ratex.currency.data.source

import kotlin.time.Duration
import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

/**
 * Первичный источник курсов. Знает условия источника: откуда брать, в какой валюте он считает и как часто его имеет смысл опрашивать.
 * Кэш и пересчёт под выбранную пользователем базу — задача репозитория и Domain.
 */
interface CurrencyRateSource {
    /** Стабильный ключ источника: под ним снимок хранится в [ru.fasdev.ratex.currency.data.storage.RateSnapshotStorage]. */
    val id: String

    /** Валюта, к которой источник публикует курсы. */
    val baseCode: String

    /** Через сколько снимок считается устаревшим. */
    val refreshInterval: Duration

    suspend fun fetch(): RateSnapshotDomain
}
