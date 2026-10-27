package com.example.engine

import androidx.annotation.DrawableRes
import com.example.R
import com.example.model.MediaCategory

enum class KuaishouPostType {
    VIDEO,
    PHOTO_CAROUSEL
}

data class KuaishouVideoQualityStream(
    val qualityId: String,
    val qualityLabel: String,
    val shortBadge: String,
    val resolution: String,
    val bitrateKbps: Int,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val videoUrl: String,
    val isWatermarkFree: Boolean = true
)

data class KuaishouAudioStream(
    val audioUrl: String,
    val format: String = "MP3",
    val bitrateKbps: Int = 320,
    val durationText: String = "02:45",
    val fileSizeBytes: Long = 6_450_000L,
    val formattedSize: String = "6.2 MB",
    val soundtrackTitle: String = "Original Kuaishou Soundtrack"
)

data class KuaishouPhotoItem(
    val id: String,
    val index: Int,
    val imageUrl: String,
    val resolution: String = "2160x2880",
    val fileSizeBytes: Long = 2_850_000L,
    val formattedSize: String = "2.7 MB",
    @DrawableRes val thumbnailRes: Int = R.drawable.vid_thumb_1
)

data class KuaishouParsedPost(
    val postId: String,
    val cleanShareUrl: String,
    val resolvedPageUrl: String,
    val rawInput: String,
    val postType: KuaishouPostType,
    val title: String,
    val authorName: String,
    val authorId: String,
    val durationText: String,
    val coverUrl: String,
    val coverFileSizeBytes: Long = 1_450_000L,
    val coverFormattedSize: String = "1.4 MB",
    @DrawableRes val thumbnailRes: Int = R.drawable.vid_thumb_1,
    val availableQualities: List<KuaishouVideoQualityStream> = emptyList(),
    val audioStream: KuaishouAudioStream,
    val photoCarousel: List<KuaishouPhotoItem> = emptyList(),
    val parsedViaSource: String = "KS-Downloader Engine",
    val sessionDid: String = ""
) {
    fun buildCleanFileName(extension: String, qualityTag: String): String {
        val safeAuthor = authorName
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "")
            .take(18)
            .ifBlank { "Creator" }
        val safeId = postId
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(12)
            .ifBlank { "KS8842" }
        val safeQuality = qualityTag
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(12)
            .ifBlank { "HD" }
        val cleanExt = extension.trimStart('.').lowercase()
        return "KuaiSave_${safeAuthor}_${safeId}_${safeQuality}.$cleanExt"
    }
}

data class KuaishouRuleConfig(
    val version: String,
    val remoteConfigUrl: String,
    val mobileUserAgent: String,
    val desktopUserAgent: String,
    val shareUrlPattern: String,
    val initialStatePattern: String,
    val pageDataPattern: String,
    val apolloStatePattern: String,
    val videoKeys: List<String>,
    val audioKeys: List<String>,
    val coverKeys: List<String>,
    val photoAtlasKeys: List<String>
)

enum class DownloadTaskStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadTaskState(
    val taskId: String = "",
    val fileName: String = "",
    val targetLabel: String = "",
    val sourceUrl: String = "",
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMbps: Double = 0.0,
    val formattedSpeed: String = "0.0 MB/s",
    val status: DownloadTaskStatus = DownloadTaskStatus.IDLE,
    val savedFilePath: String? = null,
    val mediaCategory: MediaCategory = MediaCategory.VIDEOS
)
