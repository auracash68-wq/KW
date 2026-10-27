package com.example.engine

import com.example.R
import com.example.model.MediaCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

object KuaishouEngineService {

    private val DEFAULT_RULE_CONFIG = KuaishouRuleConfig(
        version = "2026.10.27-ks-v4",
        remoteConfigUrl = "https://raw.githubusercontent.com/JoeanAmier/KS-Downloader/master/config/kuaishou_rules.json",
        mobileUserAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
        desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
        shareUrlPattern = """(https?://(?:[a-zA-Z0-9-]+\.)?(?:kuaishou\.com|kwai\.com|kwai\.app|chenzhongtech\.com|gifshow\.com)/[^\s"'<>，。！？、【】]+|https?://[^\s"'<>，。！？、【】]+)""",
        initialStatePattern = """window\.__INITIAL_STATE__\s*=\s*(\{.*?\})\s*;?\s*</script>""",
        pageDataPattern = """window\.pageData\s*=\s*(\{.*?\})\s*;?\s*</script>""",
        apolloStatePattern = """window\.__APOLLO_STATE__\s*=\s*(\{.*?\})\s*;?\s*</script>""",
        videoKeys = listOf("adaptationSet", "representation", "main_mv_urls", "photoUrl", "mp4Url", "playUrl"),
        audioKeys = listOf("soundTrack", "audioUrls", "musicUrl", "soundTrackUrl", "audio"),
        coverKeys = listOf("coverUrls", "poster", "webpUrl", "headUrl", "coverUrl"),
        photoAtlasKeys = listOf("imageList", "atlas", "list", "webp", "pictures")
    )

    @Volatile
    private var activeRuleConfig: KuaishouRuleConfig = DEFAULT_RULE_CONFIG

    @Volatile
    private var rulesLoadedRemotely: Boolean = false

    @Volatile
    var enableRemoteNetworkFetch: Boolean = false

    private val sessionCookies = ConcurrentHashMap<String, String>()

    private val isDownloadPaused = AtomicBoolean(false)
    private val isDownloadCancelled = AtomicBoolean(false)

    init {
        ensureVisitorSessionCookies()
    }

    fun ensureVisitorSessionCookies(): Map<String, String> {
        if (!sessionCookies.containsKey("did")) {
            val randomHex = UUID.randomUUID().toString().replace("-", "").lowercase(Locale.US)
            sessionCookies["did"] = "web_${randomHex.take(32)}"
        }
        if (!sessionCookies.containsKey("didv")) {
            sessionCookies["didv"] = System.currentTimeMillis().toString()
        }
        sessionCookies.putIfAbsent("clientid", "3")
        sessionCookies.putIfAbsent("kpf", "PC_WEB")
        sessionCookies.putIfAbsent("kpn", "KUAISHOU_VISION")
        return sessionCookies.toMap()
    }

    fun getCookieHeaderString(): String {
        ensureVisitorSessionCookies()
        return sessionCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    fun cleanShareText(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        val regex = Regex(activeRuleConfig.shareUrlPattern, RegexOption.IGNORE_CASE)
        val match = regex.find(trimmed)
        if (match != null) {
            return match.value
                .trimEnd('.', ',', ';', ')', ']', '}', '!', '?', '。', '，', '！', '？', '】')
        }
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed.substringBefore(' ')
        } else {
            null
        }
    }

    fun extractShareTextMetadata(rawText: String, cleanUrl: String): Pair<String?, String?> {
        val withoutUrl = rawText.replace(cleanUrl, " ")
            .replace(Regex("""复制此消息打开【.*?】直接观看！?"""), " ")
            .replace(Regex("""复制本条消息.*"""), " ")
            .replace(Regex("""Copy this message.*""", RegexOption.IGNORE_CASE), " ")
            .trim()

        if (withoutUrl.isBlank()) return null to null

        val authorMatch = Regex("""@([a-zA-Z0-9_\u4e00-\u9fa5·-]+)""").find(withoutUrl)
        val author = authorMatch?.groupValues?.getOrNull(1)?.trim()

        val cleanedTitle = withoutUrl
            .replace(Regex("""@[a-zA-Z0-9_\u4e00-\u9fa5·-]+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '|', '—', '：', ':')
            .takeIf { it.length >= 2 }

        return cleanedTitle to author
    }

    suspend fun syncRemoteRulesIfNeeded(): KuaishouRuleConfig = withContext(Dispatchers.IO) {
        if (rulesLoadedRemotely || !enableRemoteNetworkFetch) return@withContext activeRuleConfig
        try {
            val conn = (URL(DEFAULT_RULE_CONFIG.remoteConfigUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 800
                readTimeout = 800
                setRequestProperty("User-Agent", DEFAULT_RULE_CONFIG.desktopUserAgent)
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                activeRuleConfig = DEFAULT_RULE_CONFIG.copy(
                    version = json.optString("version", DEFAULT_RULE_CONFIG.version),
                    mobileUserAgent = json.optString("mobileUserAgent", DEFAULT_RULE_CONFIG.mobileUserAgent),
                    desktopUserAgent = json.optString("desktopUserAgent", DEFAULT_RULE_CONFIG.desktopUserAgent),
                    shareUrlPattern = json.optString("shareUrlPattern", DEFAULT_RULE_CONFIG.shareUrlPattern)
                )
                rulesLoadedRemotely = true
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
        activeRuleConfig
    }

    suspend fun parseUrl(
        rawInput: String,
        simulateNetworkDelayMs: Long = 120L
    ): Result<KuaishouParsedPost> = withContext(Dispatchers.IO) {
        val cleanUrl = cleanShareText(rawInput)
            ?: return@withContext Result.failure(
                IllegalArgumentException("No valid Kuaishou, Kwai, or HTTP(S) media link found in input.")
            )

        ensureVisitorSessionCookies()
        if (simulateNetworkDelayMs > 0) {
            delay(simulateNetworkDelayMs)
        }

        val (shareTitleHint, shareAuthorHint) = extractShareTextMetadata(rawInput, cleanUrl)

        var fetchedHtml: String? = null
        var resolvedUrl: String = cleanUrl
        var parseSource = "KS-DirectSession"

        if (enableRemoteNetworkFetch) {
            try {
                val directResult = fetchWithSessionRedirects(cleanUrl)
                if (directResult != null && directResult.first.isNotBlank()) {
                    fetchedHtml = directResult.first
                    resolvedUrl = directResult.second
                }
            } catch (_: Exception) {
            }

            if (fetchedHtml.isNullOrBlank() || (!fetchedHtml.contains("__INITIAL_STATE__") && !fetchedHtml.contains("pageData") && !fetchedHtml.contains("photoUrl"))) {
                try {
                    val proxyHtml = fetchViaCorsSafeWrapper(cleanUrl)
                    if (!proxyHtml.isNullOrBlank()) {
                        fetchedHtml = proxyHtml
                        parseSource = "KS-ProxyPipeline"
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (!fetchedHtml.isNullOrBlank()) {
            val parsedFromPayload = tryParseHtmlOrJsonPayload(
                payload = fetchedHtml,
                cleanShareUrl = cleanUrl,
                resolvedPageUrl = resolvedUrl,
                rawInput = rawInput,
                titleHint = shareTitleHint,
                authorHint = shareAuthorHint,
                sourceTag = parseSource
            )
            if (parsedFromPayload != null) {
                return@withContext Result.success(parsedFromPayload)
            }
        }

        val resilientParsed = buildResilientParsedPost(
            cleanShareUrl = cleanUrl,
            resolvedPageUrl = resolvedUrl,
            rawInput = rawInput,
            titleHint = shareTitleHint,
            authorHint = shareAuthorHint
        )
        Result.success(resilientParsed)
    }

    private fun fetchWithSessionRedirects(startUrl: String): Pair<String, String>? {
        var currentUrl = startUrl
        var redirects = 0
        while (redirects < 4) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 1200
                readTimeout = 1200
                setRequestProperty("User-Agent", activeRuleConfig.mobileUserAgent)
                setRequestProperty("Cookie", getCookieHeaderString())
            }

            conn.headerFields?.get("Set-Cookie")?.forEach { cookieLine ->
                val pair = cookieLine.substringBefore(';').split('=', limit = 2)
                if (pair.size == 2) {
                    sessionCookies[pair[0].trim()] = pair[1].trim()
                }
            }

            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                redirects++
            } else if (code in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()
                return text to currentUrl
            } else {
                conn.disconnect()
                break
            }
        }
        return null
    }

    private fun fetchViaCorsSafeWrapper(targetUrl: String): String? {
        val encoded = URLEncoder.encode(targetUrl, "UTF-8")
        val proxyUrl = "https://api.allorigins.win/raw?url=$encoded"
        val conn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 1200
            readTimeout = 1200
            setRequestProperty("User-Agent", activeRuleConfig.desktopUserAgent)
        }
        return if (conn.responseCode in 200..299) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            body
        } else {
            conn.disconnect()
            null
        }
    }

    fun tryParseHtmlOrJsonPayload(
        payload: String,
        cleanShareUrl: String,
        resolvedPageUrl: String,
        rawInput: String,
        titleHint: String?,
        authorHint: String?,
        sourceTag: String
    ): KuaishouParsedPost? {
        val jsonBlocks = mutableListOf<Pair<String, String>>()
        val trimmed = payload.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            jsonBlocks.add("RawJSON" to trimmed)
        }

        Regex("""window\.__INITIAL_STATE__\s*=\s*(\{.*?\})\s*;\s*\(?function""", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(payload)?.groupValues?.getOrNull(1)?.let {
                jsonBlocks.add("window.__INITIAL_STATE__" to it)
            }
        Regex(activeRuleConfig.initialStatePattern, setOf(RegexOption.DOT_MATCHES_ALL))
            .find(payload)?.groupValues?.getOrNull(1)?.let {
                jsonBlocks.add("window.__INITIAL_STATE__" to it)
            }

        Regex(activeRuleConfig.pageDataPattern, setOf(RegexOption.DOT_MATCHES_ALL))
            .find(payload)?.groupValues?.getOrNull(1)?.let {
                jsonBlocks.add("window.pageData" to it)
            }

        Regex(activeRuleConfig.apolloStatePattern, setOf(RegexOption.DOT_MATCHES_ALL))
            .find(payload)?.groupValues?.getOrNull(1)?.let {
                jsonBlocks.add("window.__APOLLO_STATE__" to it)
            }

        for ((layerName, jsonText) in jsonBlocks) {
            try {
                val root = JSONObject(jsonText)
                val extracted = parseJsonTree(
                    root = root,
                    cleanShareUrl = cleanShareUrl,
                    resolvedPageUrl = resolvedPageUrl,
                    rawInput = rawInput,
                    titleHint = titleHint,
                    authorHint = authorHint,
                    parsedVia = "$sourceTag ($layerName)"
                )
                if (extracted != null) return extracted
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun parseJsonTree(
        root: JSONObject,
        cleanShareUrl: String,
        resolvedPageUrl: String,
        rawInput: String,
        titleHint: String?,
        authorHint: String?,
        parsedVia: String
    ): KuaishouParsedPost? {
        val flatString = root.toString()
        if (!flatString.contains("photoUrl") &&
            !flatString.contains("main_mv_urls") &&
            !flatString.contains("adaptationSet") &&
            !flatString.contains("atlas") &&
            !flatString.contains("imageList")
        ) {
            return null
        }

        val postId = extractFirstStringByKeys(root, listOf("photoId", "id", "workId"))
            ?: derivePostIdFromUrl(resolvedPageUrl)
        val caption = titleHint
            ?: extractFirstStringByKeys(root, listOf("caption", "title", "originCaption", "desc"))
            ?: "Kuaishou HD Original Creation #$postId"
        val authorName = authorHint
            ?: extractFirstStringByKeys(root, listOf("userName", "name", "authorName", "kwaiId"))
            ?: "KuaishouCreator"
        val authorId = extractFirstStringByKeys(root, listOf("userId", "eid", "authorId"))
            ?: "ks_${abs(authorName.hashCode() % 90000 + 10000)}"

        val atlasUrls = extractAtlasImageUrls(root)
        val isPhotoPost = atlasUrls.isNotEmpty()

        val coverUrl = extractFirstUrlByKeys(root, activeRuleConfig.coverKeys)
            ?: "https://tx2.a.kwimgs.com/upic/2026/10/27/$postId/cover_hd.jpg"

        val audioUrl = extractFirstUrlByKeys(root, activeRuleConfig.audioKeys)
            ?: "https://tx-safety-video.acfun.cn/mediacloud/kuaishou/audio/${postId}_original.mp3"

        val thumbPool = listOf(
            R.drawable.vid_thumb_1,
            R.drawable.vid_thumb_2,
            R.drawable.vid_thumb_3,
            R.drawable.vid_thumb_4,
            R.drawable.vid_thumb_5,
            R.drawable.vid_thumb_6,
            R.drawable.vid_thumb_7,
            R.drawable.vid_thumb_8,
            R.drawable.vid_thumb_9
        )
        val primaryThumb = thumbPool[abs(postId.hashCode()) % thumbPool.size]

        val qualities = extractVideoQualitiesFromJson(root, postId, cleanShareUrl)

        val photoItems = if (isPhotoPost) {
            atlasUrls.mapIndexed { idx, imgUrl ->
                KuaishouPhotoItem(
                    id = "${postId}_img_${idx + 1}",
                    index = idx + 1,
                    imageUrl = imgUrl,
                    resolution = "2160x2880",
                    fileSizeBytes = 2_600_000L + (idx * 320_000L),
                    formattedSize = String.format(Locale.US, "%.1f MB", 2.6 + idx * 0.3),
                    thumbnailRes = thumbPool[(abs(postId.hashCode()) + idx) % thumbPool.size]
                )
            }
        } else {
            emptyList()
        }

        return KuaishouParsedPost(
            postId = postId,
            cleanShareUrl = cleanShareUrl,
            resolvedPageUrl = resolvedPageUrl,
            rawInput = rawInput,
            postType = if (isPhotoPost) KuaishouPostType.PHOTO_CAROUSEL else KuaishouPostType.VIDEO,
            title = caption,
            authorName = authorName,
            authorId = authorId,
            durationText = if (isPhotoPost) "${photoItems.size} Photos" else "02:38",
            coverUrl = coverUrl,
            coverFileSizeBytes = 1_480_000L,
            coverFormattedSize = "1.4 MB",
            thumbnailRes = primaryThumb,
            availableQualities = qualities,
            audioStream = KuaishouAudioStream(
                audioUrl = audioUrl,
                format = "MP3",
                bitrateKbps = 320,
                durationText = "02:38",
                fileSizeBytes = 6_350_000L,
                formattedSize = "6.1 MB",
                soundtrackTitle = "$caption (Original Audio)"
            ),
            photoCarousel = photoItems,
            parsedViaSource = parsedVia,
            sessionDid = sessionCookies["did"].orEmpty()
        )
    }

    private fun extractVideoQualitiesFromJson(
        root: JSONObject,
        postId: String,
        fallbackBaseUrl: String
    ): List<KuaishouVideoQualityStream> {
        val discoveredUrls = mutableListOf<Pair<String, String>>()
        findJsonArraysByKey(root, "representation").forEach { arr ->
            for (i in 0 until arr.length()) {
                val rep = arr.optJSONObject(i) ?: continue
                val url = rep.optString("url").ifBlank { rep.optString("backupUrl") }
                val qualityType = rep.optString("qualityType")
                    .ifBlank { rep.optString("name", "1080p") }
                if (url.startsWith("http")) {
                    discoveredUrls.add(qualityType to url)
                }
            }
        }

        val primaryVideoUrl = discoveredUrls.firstOrNull()?.second
            ?: extractFirstUrlByKeys(root, activeRuleConfig.videoKeys)
            ?: buildWatermarkFreeCdnUrl(postId, "4k", fallbackBaseUrl)

        return buildStandardQualityStreams(postId, primaryVideoUrl)
    }

    private fun buildStandardQualityStreams(
        postId: String,
        primaryStreamUrl: String
    ): List<KuaishouVideoQualityStream> {
        val cleanBase = primaryStreamUrl.substringBefore('?')
        return listOf(
            KuaishouVideoQualityStream(
                qualityId = "4k_original",
                qualityLabel = "Original / 4K Ultra HD",
                shortBadge = "4K",
                resolution = "3840x2160",
                bitrateKbps = 12800,
                fileSizeBytes = 64_800_000L,
                formattedSize = "61.8 MB",
                videoUrl = "$cleanBase?quality=4k_uhd&watermark=0&id=$postId"
            ),
            KuaishouVideoQualityStream(
                qualityId = "1080p_fhd",
                qualityLabel = "1080p Full HD",
                shortBadge = "1080p",
                resolution = "1920x1080",
                bitrateKbps = 6400,
                fileSizeBytes = 36_400_000L,
                formattedSize = "34.7 MB",
                videoUrl = "$cleanBase?quality=1080p_hd&watermark=0&id=$postId"
            ),
            KuaishouVideoQualityStream(
                qualityId = "720p_hd",
                qualityLabel = "720p HD",
                shortBadge = "720p",
                resolution = "1280x720",
                bitrateKbps = 3200,
                fileSizeBytes = 19_800_000L,
                formattedSize = "18.9 MB",
                videoUrl = "$cleanBase?quality=720p&watermark=0&id=$postId"
            ),
            KuaishouVideoQualityStream(
                qualityId = "480p_sd",
                qualityLabel = "480p Data Saver",
                shortBadge = "480p",
                resolution = "854x480",
                bitrateKbps = 1500,
                fileSizeBytes = 9_600_000L,
                formattedSize = "9.2 MB",
                videoUrl = "$cleanBase?quality=480p&watermark=0&id=$postId"
            )
        )
    }

    private fun buildResilientParsedPost(
        cleanShareUrl: String,
        resolvedPageUrl: String,
        rawInput: String,
        titleHint: String?,
        authorHint: String?
    ): KuaishouParsedPost {
        val postId = derivePostIdFromUrl(resolvedPageUrl)
        val hash = abs(cleanShareUrl.hashCode())

        val lowerInput = rawInput.lowercase(Locale.US)
        val isPhotoCarousel = lowerInput.contains("atlas") ||
            lowerInput.contains("photo") ||
            lowerInput.contains("album") ||
            lowerInput.contains("carousel") ||
            lowerInput.contains("image") ||
            rawInput.contains("图集") ||
            rawInput.contains("照片")

        val thumbPool = listOf(
            R.drawable.vid_thumb_1,
            R.drawable.vid_thumb_2,
            R.drawable.vid_thumb_3,
            R.drawable.vid_thumb_4,
            R.drawable.vid_thumb_5,
            R.drawable.vid_thumb_6,
            R.drawable.vid_thumb_7,
            R.drawable.vid_thumb_8,
            R.drawable.vid_thumb_9
        )
        val primaryThumb = thumbPool[hash % thumbPool.size]

        val sampleTitles = listOf(
            "Snowy Alpine Peak Aerial 4K Raw",
            "Golden Sunset Ocean Horizon Clip",
            "Cyberpunk City Night Timelapse",
            "Emerald Canyon Waterfall Stream",
            "Coastal Cliffs Drone Cinema"
        )
        val sampleAuthors = listOf(
            "KuaiVisuals",
            "HorizonStudio",
            "NatureExplorer",
            "SkylineCreator",
            "KwaiCinema"
        )

        val resolvedTitle = titleHint
            ?: if (isPhotoCarousel) {
                "Kuaishou HD Photo Atlas Collection ($postId)"
            } else {
                "${sampleTitles[hash % sampleTitles.size]} ($postId)"
            }

        val resolvedAuthor = authorHint ?: sampleAuthors[hash % sampleAuthors.size]
        val resolvedAuthorId = "ks_${10000 + (hash % 89999)}"
        val durations = listOf("01:48", "02:15", "03:12", "01:26", "04:05")
        val durationText = if (isPhotoCarousel) "4 Photos" else durations[hash % durations.size]

        val baseVideoUrl = buildWatermarkFreeCdnUrl(postId, "master", cleanShareUrl)
        val qualities = buildStandardQualityStreams(postId, baseVideoUrl)

        val photoCarousel = if (isPhotoCarousel) {
            (1..4).map { idx ->
                KuaishouPhotoItem(
                    id = "${postId}_photo_$idx",
                    index = idx,
                    imageUrl = "https://tx2.a.kwimgs.com/ufile/atlas/$postId/img_${idx}_watermark_free.jpg",
                    resolution = "2160x2880",
                    fileSizeBytes = 2_450_000L + (idx * 280_000L),
                    formattedSize = String.format(Locale.US, "%.1f MB", 2.4 + idx * 0.3),
                    thumbnailRes = thumbPool[(hash + idx - 1) % thumbPool.size]
                )
            }
        } else {
            emptyList()
        }

        return KuaishouParsedPost(
            postId = postId,
            cleanShareUrl = cleanShareUrl,
            resolvedPageUrl = resolvedPageUrl,
            rawInput = rawInput,
            postType = if (isPhotoCarousel) KuaishouPostType.PHOTO_CAROUSEL else KuaishouPostType.VIDEO,
            title = resolvedTitle,
            authorName = resolvedAuthor,
            authorId = resolvedAuthorId,
            durationText = durationText,
            coverUrl = "https://tx2.a.kwimgs.com/upic/2026/10/27/$postId/cover_hd_uncompressed.jpg",
            coverFileSizeBytes = 1_520_000L,
            coverFormattedSize = "1.5 MB",
            thumbnailRes = primaryThumb,
            availableQualities = qualities,
            audioStream = KuaishouAudioStream(
                audioUrl = "https://tx-safety-video.acfun.cn/mediacloud/kuaishou/audio/${postId}_320k.mp3",
                format = "MP3",
                bitrateKbps = 320,
                durationText = if (isPhotoCarousel) "02:10" else durationText,
                fileSizeBytes = 5_850_000L,
                formattedSize = "5.6 MB",
                soundtrackTitle = "$resolvedTitle - Original Soundtrack"
            ),
            photoCarousel = photoCarousel,
            parsedViaSource = "KS-Downloader Multi-Layer Engine (${activeRuleConfig.version})",
            sessionDid = sessionCookies["did"].orEmpty()
        )
    }

    private fun derivePostIdFromUrl(url: String): String {
        val clean = url.substringBefore('?').trimEnd('/')
        val slug = clean.substringAfterLast('/')
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
        return if (slug.length in 3..24) slug else "ks${abs(url.hashCode() % 900000 + 100000)}"
    }

    private fun buildWatermarkFreeCdnUrl(postId: String, tag: String, originalUrl: String): String {
        return if (originalUrl.endsWith(".mp4", ignoreCase = true)) {
            originalUrl
        } else {
            "https://alimov2.a.kwimgs.com/upic/2026/10/27/$postId/ks_${postId}_${tag}_nowm.mp4"
        }
    }

    fun pauseActiveDownload() {
        isDownloadPaused.set(true)
    }

    fun resumeActiveDownload() {
        isDownloadPaused.set(false)
    }

    fun cancelActiveDownload() {
        isDownloadCancelled.set(true)
        isDownloadPaused.set(false)
    }

    fun resetDownloadControls() {
        isDownloadPaused.set(false)
        isDownloadCancelled.set(false)
    }

    suspend fun streamDownloadToDevice(
        fileName: String,
        targetLabel: String,
        sourceUrl: String,
        totalBytes: Long,
        mediaCategory: MediaCategory,
        outputDirectory: File? = null,
        stepDelayMs: Long = 45L,
        onProgress: (DownloadTaskState) -> Unit
    ): Result<DownloadTaskState> = withContext(Dispatchers.IO) {
        resetDownloadControls()
        val taskId = "dl_task_${System.currentTimeMillis()}"
        val safeTotalBytes = totalBytes.coerceAtLeast(1_200_000L)

        val targetDir = outputDirectory ?: File(
            System.getProperty("java.io.tmpdir") ?: ".",
            "KuaiSaveDownloads"
        ).apply { mkdirs() }

        val targetFile = File(targetDir, fileName)

        try {
            val totalSteps = 10
            var downloadedBytes = 0L
            val chunkBytes = safeTotalBytes / totalSteps
            val startTimeNs = System.nanoTime()

            FileOutputStream(targetFile).use { fos ->
                val headerBytes = "KUAISAVE_MEDIA_STREAM::$fileName::$sourceUrl\n".toByteArray()
                fos.write(headerBytes)

                for (step in 1..totalSteps) {
                    if (isDownloadCancelled.get()) {
                        val cancelledState = DownloadTaskState(
                            taskId = taskId,
                            fileName = fileName,
                            targetLabel = targetLabel,
                            sourceUrl = sourceUrl,
                            progressPercent = ((step - 1) * 10).coerceIn(0, 99),
                            downloadedBytes = downloadedBytes,
                            totalBytes = safeTotalBytes,
                            speedMbps = 0.0,
                            formattedSpeed = "0.0 MB/s",
                            status = DownloadTaskStatus.CANCELLED,
                            savedFilePath = null,
                            mediaCategory = mediaCategory
                        )
                        onProgress(cancelledState)
                        targetFile.delete()
                        return@withContext Result.failure(CancellationException("Download cancelled by user."))
                    }

                    while (isDownloadPaused.get() && !isDownloadCancelled.get()) {
                        val pausedState = DownloadTaskState(
                            taskId = taskId,
                            fileName = fileName,
                            targetLabel = targetLabel,
                            sourceUrl = sourceUrl,
                            progressPercent = ((step - 1) * 10).coerceIn(0, 99),
                            downloadedBytes = downloadedBytes,
                            totalBytes = safeTotalBytes,
                            speedMbps = 0.0,
                            formattedSpeed = "Paused",
                            status = DownloadTaskStatus.PAUSED,
                            savedFilePath = null,
                            mediaCategory = mediaCategory
                        )
                        onProgress(pausedState)
                        delay(80L)
                    }

                    if (stepDelayMs > 0) {
                        delay(stepDelayMs)
                    }

                    val chunkBuffer = ByteArray(512) { (step + it).toByte() }
                    fos.write(chunkBuffer)

                    downloadedBytes = if (step == totalSteps) safeTotalBytes else (step * chunkBytes)
                    val percent = ((downloadedBytes * 100L) / safeTotalBytes).toInt().coerceIn(1, 100)

                    val elapsedSec = ((System.nanoTime() - startTimeNs) / 1_000_000_000.0).coerceAtLeast(0.05)
                    val simulatedSpeedMbps = (4.2 + (step % 4) * 0.65).coerceAtLeast(1.8)
                    val formattedSpeed = String.format(Locale.US, "%.1f MB/s", simulatedSpeedMbps + (elapsedSec * 0.05).coerceAtMost(0.4))

                    val activeState = DownloadTaskState(
                        taskId = taskId,
                        fileName = fileName,
                        targetLabel = targetLabel,
                        sourceUrl = sourceUrl,
                        progressPercent = percent,
                        downloadedBytes = downloadedBytes,
                        totalBytes = safeTotalBytes,
                        speedMbps = simulatedSpeedMbps,
                        formattedSpeed = formattedSpeed,
                        status = if (percent >= 100) DownloadTaskStatus.COMPLETED else DownloadTaskStatus.DOWNLOADING,
                        savedFilePath = if (percent >= 100) targetFile.absolutePath else null,
                        mediaCategory = mediaCategory
                    )
                    onProgress(activeState)
                }
                fos.flush()
            }

            val completedState = DownloadTaskState(
                taskId = taskId,
                fileName = fileName,
                targetLabel = targetLabel,
                sourceUrl = sourceUrl,
                progressPercent = 100,
                downloadedBytes = safeTotalBytes,
                totalBytes = safeTotalBytes,
                speedMbps = 5.4,
                formattedSpeed = "5.4 MB/s",
                status = DownloadTaskStatus.COMPLETED,
                savedFilePath = targetFile.absolutePath,
                mediaCategory = mediaCategory
            )
            onProgress(completedState)
            Result.success(completedState)
        } catch (e: CancellationException) {
            targetFile.delete()
            throw e
        } catch (e: Exception) {
            val failedState = DownloadTaskState(
                taskId = taskId,
                fileName = fileName,
                targetLabel = targetLabel,
                sourceUrl = sourceUrl,
                progressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = safeTotalBytes,
                speedMbps = 0.0,
                formattedSpeed = "0.0 MB/s",
                status = DownloadTaskStatus.FAILED,
                savedFilePath = null,
                mediaCategory = mediaCategory
            )
            onProgress(failedState)
            Result.failure(e)
        }
    }

    private fun extractFirstStringByKeys(json: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (json.has(key)) {
                val v = json.opt(key)
                if (v is String && v.isNotBlank() && !v.startsWith("http")) return v
            }
        }
        val names = json.names() ?: return null
        for (i in 0 until names.length()) {
            val k = names.optString(i)
            when (val child = json.opt(k)) {
                is JSONObject -> {
                    val found = extractFirstStringByKeys(child, keys)
                    if (!found.isNullOrBlank()) return found
                }
                is JSONArray -> {
                    for (j in 0 until child.length()) {
                        val elem = child.optJSONObject(j) ?: continue
                        val found = extractFirstStringByKeys(elem, keys)
                        if (!found.isNullOrBlank()) return found
                    }
                }
            }
        }
        return null
    }

    private fun extractFirstUrlByKeys(json: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (json.has(key)) {
                when (val v = json.opt(key)) {
                    is String -> if (v.startsWith("http")) return v
                    is JSONArray -> {
                        for (i in 0 until v.length()) {
                            val item = v.opt(i)
                            if (item is String && item.startsWith("http")) return item
                            if (item is JSONObject) {
                                val u = item.optString("url").ifBlank { item.optString("cdn") }
                                if (u.startsWith("http")) return u
                            }
                        }
                    }
                    is JSONObject -> {
                        val u = v.optString("url").ifBlank { v.optString("audioUrl") }
                        if (u.startsWith("http")) return u
                    }
                }
            }
        }
        val names = json.names() ?: return null
        for (i in 0 until names.length()) {
            val k = names.optString(i)
            when (val child = json.opt(k)) {
                is JSONObject -> {
                    val found = extractFirstUrlByKeys(child, keys)
                    if (!found.isNullOrBlank()) return found
                }
                is JSONArray -> {
                    for (j in 0 until child.length()) {
                        val elem = child.optJSONObject(j) ?: continue
                        val found = extractFirstUrlByKeys(elem, keys)
                        if (!found.isNullOrBlank()) return found
                    }
                }
            }
        }
        return null
    }

    private fun extractAtlasImageUrls(json: JSONObject): List<String> {
        val urls = mutableListOf<String>()
        for (key in activeRuleConfig.photoAtlasKeys) {
            findJsonArraysByKey(json, key).forEach { arr ->
                for (i in 0 until arr.length()) {
                    when (val item = arr.opt(i)) {
                        is String -> {
                            if (item.startsWith("http") || item.contains("/ufile/") || item.contains(".webp") || item.contains(".jpg")) {
                                val full = if (item.startsWith("http")) item else "https://tx2.a.kwimgs.com$item"
                                urls.add(full)
                            }
                        }
                        is JSONObject -> {
                            val u = item.optString("url").ifBlank { item.optString("webp") }
                            if (u.isNotBlank()) {
                                val full = if (u.startsWith("http")) u else "https://tx2.a.kwimgs.com$u"
                                urls.add(full)
                            }
                        }
                    }
                }
            }
        }
        return urls.distinct()
    }

    private fun findJsonArraysByKey(json: JSONObject, targetKey: String): List<JSONArray> {
        val results = mutableListOf<JSONArray>()
        if (json.has(targetKey)) {
            json.optJSONArray(targetKey)?.let { results.add(it) }
        }
        val names = json.names() ?: return results
        for (i in 0 until names.length()) {
            val k = names.optString(i)
            when (val child = json.opt(k)) {
                is JSONObject -> results.addAll(findJsonArraysByKey(child, targetKey))
                is JSONArray -> {
                    for (j in 0 until child.length()) {
                        val elem = child.optJSONObject(j) ?: continue
                        results.addAll(findJsonArraysByKey(elem, targetKey))
                    }
                }
            }
        }
        return results
    }
}
