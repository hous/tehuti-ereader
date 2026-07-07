package com.tehuti.reader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC, title ASC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query(
        """
        SELECT books.*, reading_positions.progression AS progression
        FROM books LEFT JOIN reading_positions ON books.id = reading_positions.bookId
        ORDER BY books.lastOpenedAt DESC, books.title ASC
        """,
    )
    fun observeAllWithProgression(): Flow<List<BookWithProgression>>

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE books SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpenedAt(id: String, timestamp: Long)
}
