package com.tehuti.reader.domain.repo

import com.tehuti.reader.domain.model.LookupResult

interface LookupRepository {
    suspend fun lookupDictionary(word: String): LookupResult.Dictionary?
    suspend fun lookupWikipedia(term: String): LookupResult.Wikipedia?
}
