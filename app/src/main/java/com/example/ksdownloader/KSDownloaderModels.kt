package com.example.ksdownloader

import androidx.annotation.DrawableRes
import com.example.R

/**
 * Official work types defined in JoeanAmier/KS-Downloader (`src/extract/extractor.py`):
 * - 视频 (VIDEO)
 * - 图集 (PHOTO_ATLAS)
 * - 图片 (SINGLE_IMAGE)
 */
enum class KSWorksType(val ksLabel: String) {
    VIDEO("视频"),
    PHOTO_ATLAS("图集");

    companion object {
        fun fromKsLabel(raw: String?): KSWorksType {
            val normalized = raw?.trim().orEmpty()
            return if (
                normalized.contains("图集") ||
                normalized.contains("图片") ||
                normalized.equals("atlas", ignoreCase = true) ||
                normalized.equals("photo", ignoreCase = true) ||
                normalized.equals("image", ignoreCase = true)
            ) {
                PHOTO_ATLAS
            } else {
                VIDEO
            }
        }
    }
}

/**
 * Official 5 link formats supported by JoeanAmier/KS-Downloader (`src/link/extractor.py`).
 */
enum class KSLinkFormat(val patternDescription: String) {
    SHARE_F("https://www.kuaishou.com/f/{share-code}"),
    SHORT_V("https://v.kuaishou.com/{share-code}"),
    WEB_SHORT_VIDEO("https://www.kuaishou.com/short-video/{postsID}"),
    CN_SHORT_VIDEO("https://kuaishou.cn/short-video/{postsID}"),
    LIVE_USER_POST("https://live.kuaishou.com/u/{author-id}/{postsID}"),
    GENERIC_MEDIA("https://www.kuaishou.com/photo/{postsID}")
}

/**
 * FastAPI Port 5557 Request Schema (`python main.py api` in JoeanAmier/KS-Downloader).
 */
data class KSDownloaderFastApiRequest(
    val url: String,
    val cookie: String = "",
    val proxy: String? = null
)

data class KSVideoQualityOption(
    val qualityId: String,
    val qualityLabel: String,
    val shortBadge: String,
    val resolution: String,
    val videoUrl: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val isWatermarkFree: Boolean = true
)

data class KSMusicTrackOption(
    val musicTitle: String,
    val musicUrl: String,
    val extension: String = "mp3",
    val bitrateKbps: Int = 320,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val durationText: String
)

data class KSAtlasPhotoItem(
    val index: Int,
    val imageUrl: String,
    val resolution: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    @param:DrawableRes val thumbnailRes: Int
)

/**
 * Exact extraction model mapped from JoeanAmier/KS-Downloader (`src/extract/extractor.py`):
 * - 作品ID (worksId / Posts ID)
 * - 作品描述 (description / Description)
 * - 作品类型 (worksType / Type: 视频 or 图集)
 * - 作者昵称 (authorNickname / Author)
 * - 作者ID (authorId)
 * - 作品链接 (worksUrl)
 * - 下载地址 (downloadUrls)
 * - 封面地址 (coverUrl / Dynamic Cover)
 * - 音乐地址 (musicUrl / Posts Music File)
 */
data class KSDownloaderWorksDetail(
    val worksId: String,                   // 作品ID (Posts ID)
    val description: String,               // 作品描述 (Description)
    val worksType: KSWorksType,            // 作品类型 (Type)
    val authorNickname: String,            // 作者昵称 (Author)
    val authorId: String,                  // 作者ID (Author ID)
    val worksUrl: String,                  // 作品链接 (Clean Share/Works URL)
    val canonicalPageUrl: String,          // Resolved Kuaishou URL
    val linkFormat: KSLinkFormat,          // Matched KS-Downloader Link Format
    val downloadUrls: List<String>,        // 下载地址 (Watermark-free video or atlas URLs)
    val coverUrl: String,                  // 封面地址 (HD Cover Image URL)
    val coverFileSizeBytes: Long,
    val coverFormattedSize: String,
    val musicUrl: String,                  // 音乐地址 (Standalone MP3/M4A Audio URL)
    val musicStream: KSMusicTrackOption,
    val durationText: String,
    @param:DrawableRes val thumbnailRes: Int = R.drawable.vid_thumb_1,
    val videoQualities: List<KSVideoQualityOption> = emptyList(),
    val photoAtlas: List<KSAtlasPhotoItem> = emptyList(),
    val batchSiblingUrls: List<String> = emptyList(),
    val extractionEngineTag: String = "JoeanAmier/KS-Downloader (FastAPI:5557)"
) {
    /**
     * Generates a clean filename following JoeanAmier/KS-Downloader naming rules:
     * `{authorNickname}_{worksId}_{description}_{qualityTag}.{extension}`
     */
    fun buildKSFileName(extension: String, qualityTag: String? = null): String {
        val safeAuthor = authorNickname
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "")
            .take(14)
            .ifBlank { "KuaishouUser" }
        val safeDesc = description
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(22)
            .ifBlank { "KSWorks" }
        val safeId = worksId
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(12)
            .ifBlank { "post" }
        val suffix = if (!qualityTag.isNullOrBlank()) "_$qualityTag" else ""
        return "KS_${safeAuthor}_${safeId}_${safeDesc}${suffix}.${extension.lowercase()}"
    }

    /**
     * Returns raw dictionary map matching JoeanAmier/KS-Downloader's Chinese field keys.
     */
    fun toKSRepositoryMap(): Map<String, Any> = mapOf(
        "作品ID" to worksId,
        "作品描述" to description,
        "作品类型" to worksType.ksLabel,
        "作者昵称" to authorNickname,
        "作者ID" to authorId,
        "作品链接" to worksUrl,
        "下载地址" to downloadUrls,
        "封面地址" to coverUrl,
        "音乐地址" to musicUrl
    )
}

enum class KSDownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class KSDownloadTaskState(
    val taskId: String,
    val worksId: String,
    val fileName: String,
    val targetLabel: String,
    val sourceUrl: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val speedMbps: Double,
    val formattedSpeed: String,
    val status: KSDownloadStatus,
    val savedFilePath: String? = null
)
