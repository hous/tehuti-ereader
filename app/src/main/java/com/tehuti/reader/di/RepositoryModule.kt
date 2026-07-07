package com.tehuti.reader.di

import com.tehuti.reader.data.books.LibraryRepositoryImpl
import com.tehuti.reader.data.books.PositionRepositoryImpl
import com.tehuti.reader.domain.repo.LibraryRepository
import com.tehuti.reader.domain.repo.PositionRepository
import com.tehuti.reader.reader.format.EpubEngine
import com.tehuti.reader.reader.format.ReaderEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindPositionRepository(impl: PositionRepositoryImpl): PositionRepository

    @Binds
    abstract fun bindReaderEngine(impl: EpubEngine): ReaderEngine
}
