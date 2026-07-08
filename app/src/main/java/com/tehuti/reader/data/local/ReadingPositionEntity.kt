package com.tehuti.reader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey val bookId: String,
    val locatorJson: String,
    val anchorLocatorJson: String? = null,
    val progression: Float,
    val updatedAt: Long,
)
