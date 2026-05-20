package com.embytv.domain.model

data class MediaItemSummary(
    val id: String,
    val name: String,
    val type: String,
    val overview: String?,
    val imageUrl: String?,
)
