package com.novel.page.book.utils

fun formatWordCount(wordCount: Int): String {
    return when {
        wordCount >= 10000 -> "${wordCount / 10000}万"
        wordCount >= 1000 -> "${wordCount / 1000}千"
        else -> "$wordCount"
    }
}