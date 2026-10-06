package ru.fasdev.ratex.currency.data.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "rate_snapshot")
data class RateSnapshotEntity(@PrimaryKey val sourceId: String, val baseCode: String, val date: String, val fetchedAt: Long)

@Entity(
    tableName = "rate",
    primaryKeys = ["sourceId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = RateSnapshotEntity::class,
            parentColumns = ["sourceId"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RateEntity(val sourceId: String, val code: String, val value: Double)
