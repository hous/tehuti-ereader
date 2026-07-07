package com.tehuti.reader.di

import android.content.Context
import androidx.room.Room
import com.tehuti.reader.data.local.BookDao
import com.tehuti.reader.data.local.TehutiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTehutiDatabase(@ApplicationContext context: Context): TehutiDatabase =
        Room.databaseBuilder(context, TehutiDatabase::class.java, "tehuti.db").build()

    @Provides
    fun provideBookDao(database: TehutiDatabase): BookDao = database.bookDao()
}
