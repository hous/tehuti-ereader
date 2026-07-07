package com.tehuti.reader.data.lookup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WikipediaApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getSummary(@Path("title") title: String): WikipediaSummaryDto

    @GET("w/api.php?action=opensearch&format=json&limit=1&namespace=0")
    suspend fun openSearch(@Query("search") query: String): List<JsonElement>
}

@Serializable
data class WikipediaSummaryDto(
    val title: String,
    val extract: String,
    val thumbnail: ThumbnailDto? = null,
    @SerialName("content_urls") val contentUrls: ContentUrlsDto? = null,
)

@Serializable
data class ThumbnailDto(val source: String)

@Serializable
data class ContentUrlsDto(val desktop: DesktopDto? = null)

@Serializable
data class DesktopDto(val page: String)
