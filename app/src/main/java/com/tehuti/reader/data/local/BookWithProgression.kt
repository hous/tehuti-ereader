package com.tehuti.reader.data.local

import androidx.room.Embedded

data class BookWithProgression(
    @Embedded val book: BookEntity,
    val progression: Float?,
)
