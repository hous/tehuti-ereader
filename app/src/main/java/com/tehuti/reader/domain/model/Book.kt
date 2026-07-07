package com.tehuti.reader.domain.model

data class Book(
    val id: String,
    val sourceUri: String,
    val format: BookFormat,
    val title: String,
    val author: String? = null,
    val coverPath: String? = null,
    val progression: Float? = null,
)
