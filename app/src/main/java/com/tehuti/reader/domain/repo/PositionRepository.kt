package com.tehuti.reader.domain.repo

import com.tehuti.reader.domain.model.ReadingPosition

interface PositionRepository {
    suspend fun getPosition(bookId: String): ReadingPosition?
    suspend fun savePosition(position: ReadingPosition)
}
