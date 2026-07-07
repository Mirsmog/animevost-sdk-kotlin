package com.animevost.sdk.model

data class CommentPage(
    val newsId: Int?,
    val comments: List<AnimeComment>,
    val currentPage: Int,
    val totalPages: Int?,
)

data class AnimeComment(
    val id: Int,
    val author: CommentAuthor,
    val avatarUrl: String?,
    val userGroup: String?,
    val createdAtLabel: String?,
    val authorCommentCount: Int?,
    val ordinal: Int?,
    val bodyHtml: String,
    val bodyText: String,
    val quotes: List<CommentQuote>,
    val indentLevel: Int?,
    val depth: Int,
    val isOnline: Boolean?,
)

data class CommentAuthor(
    val name: String,
    val profileUrl: String?,
)

data class CommentQuote(
    val authorName: String?,
    val bodyHtml: String,
    val bodyText: String,
    val quotes: List<CommentQuote>,
)

data class CommentSubmissionResult(
    val newsId: Int,
    val comments: List<AnimeComment>,
    val rawMessage: String?,
)
