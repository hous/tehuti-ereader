package com.tehuti.reader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PositionDao {

    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    suspend fun getForBook(bookId: String): ReadingPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: ReadingPositionEntity)
}
