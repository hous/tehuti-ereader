package com.tehuti.reader.data.books

import com.tehuti.reader.data.local.PositionDao
import com.tehuti.reader.data.local.ReadingPositionEntity
import com.tehuti.reader.domain.model.ReadingPosition
import com.tehuti.reader.domain.repo.PositionRepository
import javax.inject.Inject

class PositionRepositoryImpl @Inject constructor(
    private val positionDao: PositionDao,
) : PositionRepository {

    override suspend fun getPosition(bookId: String): ReadingPosition? =
        positionDao.getForBook(bookId)?.toDomain()

    override suspend fun savePosition(position: ReadingPosition) {
        positionDao.upsert(
            ReadingPositionEntity(
                bookId = position.bookId,
                locatorJson = position.locatorJson,
                progression = position.progression,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

private fun ReadingPositionEntity.toDomain(): ReadingPosition = ReadingPosition(
    bookId = bookId,
    locatorJson = locatorJson,
    progression = progression,
)
