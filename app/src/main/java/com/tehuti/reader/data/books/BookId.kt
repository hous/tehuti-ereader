package com.tehuti.reader.data.books

import java.security.MessageDigest

fun bookIdFor(sourceUri: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(sourceUri.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
