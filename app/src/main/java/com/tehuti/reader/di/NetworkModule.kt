package com.tehuti.reader.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tehuti.reader.data.ai.GeminiCloudApi
import com.tehuti.reader.data.lookup.DictionaryApi
import com.tehuti.reader.data.lookup.WikipediaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "tehuti-reader/0.1 (Android EPUB reader app)")
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    @Named("dictionary")
    fun provideDictionaryRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.dictionaryapi.dev/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("wikipedia")
    fun provideWikipediaRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        // Generation can take noticeably longer than a dictionary/Wikipedia lookup.
        .client(okHttpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDictionaryApi(@Named("dictionary") retrofit: Retrofit): DictionaryApi =
        retrofit.create(DictionaryApi::class.java)

    @Provides
    @Singleton
    fun provideWikipediaApi(@Named("wikipedia") retrofit: Retrofit): WikipediaApi =
        retrofit.create(WikipediaApi::class.java)

    @Provides
    @Singleton
    fun provideGeminiCloudApi(@Named("gemini") retrofit: Retrofit): GeminiCloudApi =
        retrofit.create(GeminiCloudApi::class.java)
}
