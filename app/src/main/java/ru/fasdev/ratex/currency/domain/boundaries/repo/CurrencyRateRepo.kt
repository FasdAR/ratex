package ru.fasdev.ratex.currency.domain.boundaries.repo

import ru.fasdev.ratex.currency.domain.entity.RateSnapshotDomain

interface CurrencyRateRepo {
    suspend fun getSnapshot(): RateSnapshotDomain
}
