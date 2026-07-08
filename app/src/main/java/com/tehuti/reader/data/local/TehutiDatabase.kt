package com.tehuti.reader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BookEntity::class, ReadingPositionEntity::class], version = 3, exportSchema = false)
abstract class TehutiDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun positionDao(): PositionDao
}
