package com.tehuti.reader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BookEntity::class], version = 1, exportSchema = false)
abstract class TehutiDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
