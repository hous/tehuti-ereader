package com.tehuti.reader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val sourceUri: String,
    val format: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val addedAt: Long,
    val lastOpenedAt: Long?,
)
