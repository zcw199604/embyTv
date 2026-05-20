package com.embytv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbyItemsResponse(
    @SerializedName("Items") val items: List<EmbyItemDto> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

data class EmbyItemDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("ImageTags") val imageTags: Map<String, String>?,
)
