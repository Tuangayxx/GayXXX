package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/" to "Newest",
        "/topic/video/asian/" to "Asian",
        "/topic/video/korean/" to "Korean",
        "/topic/video/muscle/" to "Muscle",
        "/topic/video/viet-nam/" to "Vietnamese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val suffix = if (page <= 1) "" else "?page=$page"
        val doc = app.get(mainUrl + request.data + suffix).document
        val items = doc.select("a.col-video").mapNotNull { it.toSearchItem() }
        return HomePageResponse(
            listOf(HomePageList(request.name, items, isHorizontalImages = true)),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/home?search=$query").document
        return doc.select("a.col-video").mapNotNull { it.toSearchItem() }
    }

    private fun Element.toSearchItem(): SearchResponse? {
        val url = fixUrl(attr("href"))
        val name = selectFirst("p.name-video-list")?.text() ?: return null
        val image = selectFirst("img")?.let {
            it.attr("data-cfsrc").takeIf { it.isNotBlank() } ?: it.attr("src")
        }
        val duration = selectFirst(".duration")?.text().orEmpty()
        return MovieSearchResponse(
            name = name,
            url = url,
            apiName = this@Fullboys.name,
            type = TvType.NSFW,
            posterUrl = image,
            quality = getQualityFromString(duration)?.value
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val name = doc.selectFirst("h1.title-detail-media")?.text() ?: return null
        val videoUrl = doc.select("video#myvideo").attr("src")
        val posterUrl = doc.select("video#myvideo").attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        val tags = doc.select("footer .box-tag a").map { it.text() }
        val previewImages = doc.select(".gallery-row-scroll img").mapNotNull {
            it.attr("data-cfsrc").takeIf { it.isNotBlank() }
        }

        val recommendations = doc.select(".box-list-video .col-video").mapNotNull {
            val recName = it.selectFirst("p.name-video-list")?.text() ?: return@mapNotNull null
            val recUrl = fixUrl(it.attr("href"))
            val recThumb = it.selectFirst("img")?.let {
                it.attr("data-cfsrc").takeIf { it.isNotBlank() } ?: it.attr("src")
            }
            MovieSearchResponse(
                name = recName,
                url = recUrl,
                apiName = this@Fullboys.name,
                type = TvType.NSFW,
                posterUrl = recThumb
            )
        }

        return MovieLoadResponse(
            name = name,
            url = url,
            apiName = this.name,
            type = TvType.NSFW,
            dataUrl = videoUrl,
            posterUrl = posterUrl,
            backgroundPosterUrl = previewImages.firstOrNull(),
            plot = description,
            tags = tags,
            recommendations = recommendations
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback(
            ExtractorLink(
                source = name,
                name = "Fullboys Stream",
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = false
            )
        )
        return true
    }
}
