package com.tehuti.reader.domain.model

data class ReadingPosition(
    val bookId: String,
    val locatorJson: String,
    val anchorLocatorJson: String? = null,
    val progression: Float,
)
