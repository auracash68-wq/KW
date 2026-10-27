package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AppDestination {
    DOWNLOAD,
    PLAYER,
    VAULT
}

enum class MediaCategory(val label: String) {
    VIDEOS("Videos"),
    AUDIO("Audio"),
    IMAGES("Images")
}

enum class BulkContentType(
    val label: String,
    val qualities: List<String>,
    val defaultQuality: String
) {
    VIDEO(
        label = "Video",
        qualities = listOf("360p", "480p", "720p", "1080p", "4K"),
        defaultQuality = "720p"
    ),
    AUDIO(
        label = "Audio",
        qualities = listOf("64k", "128k", "192k", "256k", "320k"),
        defaultQuality = "192k"
    ),
    PHOTO(
        label = "Photo",
        qualities = listOf("SD", "HD", "Original", "PNG", "WEBP"),
        defaultQuality = "Original"
    )
}

enum class BulkItemStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class BulkPendingItem(
    val id: String,
    val title: String,
    val url: String,
    val fileSize: String,
    val duration: String,
    val quality: String,
    val contentType: BulkContentType = BulkContentType.VIDEO,
    @param:DrawableRes val thumbnailRes: Int = R.drawable.vid_thumb_1,
    val isSelected: Boolean = true,
    val status: BulkItemStatus = BulkItemStatus.PENDING,
    val progressPercent: Int = 0,
    val statusNote: String? = null
)

data class VideoMediaItem(
    val id: String,
    val title: String,
    val duration: String,
    @param:DrawableRes val thumbnailRes: Int,
    val category: MediaCategory = MediaCategory.VIDEOS,
    val isDemonstration: Boolean = true,
    val sourceUrl: String? = null,
    val fileName: String? = null,
    val qualityLabel: String? = null,
    val fileSizeText: String? = null,
    val localFilePath: String? = null,
    val authorName: String? = null
)

data class VaultMediaItem(
    val id: String,
    val title: String,
    @param:DrawableRes val thumbnailRes: Int,
    val category: MediaCategory = MediaCategory.VIDEOS,
    val isLocked: Boolean = true,
    val isDemonstration: Boolean = true,
    val duration: String = "02:38",
    val sourceUrl: String? = null,
    val fileName: String? = null,
    val qualityLabel: String? = null,
    val fileSizeText: String? = null,
    val localFilePath: String? = null
)

object ReferenceDemoCatalog {
    val initialBulkVideoItems: List<BulkPendingItem> = listOf(
        BulkPendingItem(
            id = "bulk_1",
            title = "Beautiful Nature Video",
            url = "https://youtu.be/dQw4w9WgXcQ",
            fileSize = "45.2 MB",
            duration = "12:34",
            quality = "720p",
            contentType = BulkContentType.VIDEO,
            thumbnailRes = R.drawable.vid_thumb_1,
            isSelected = true
        ),
        BulkPendingItem(
            id = "bulk_2",
            title = "Sunset Beach Video",
            url = "https://youtu.be/9bZkp7q19f0",
            fileSize = "32.6 MB",
            duration = "08:45",
            quality = "720p",
            contentType = BulkContentType.VIDEO,
            thumbnailRes = R.drawable.vid_thumb_2,
            isSelected = true
        ),
        BulkPendingItem(
            id = "bulk_3",
            title = "City Night View",
            url = "https://youtu.be/3JZ_D3ELwOQ",
            fileSize = "68.7 MB",
            duration = "15:20",
            quality = "720p",
            contentType = BulkContentType.VIDEO,
            thumbnailRes = R.drawable.vid_thumb_3,
            isSelected = true
        )
    )

    val playerVideoItems: List<VideoMediaItem> = listOf(
        VideoMediaItem("vid_1", "Snowy Mountains & Alpine Lake", "12:34", R.drawable.vid_thumb_1, MediaCategory.VIDEOS),
        VideoMediaItem("vid_2", "Golden Ocean Sunset", "08:45", R.drawable.vid_thumb_2, MediaCategory.VIDEOS),
        VideoMediaItem("vid_3", "Nighttime City Skyline", "15:20", R.drawable.vid_thumb_3, MediaCategory.VIDEOS),
        VideoMediaItem("vid_4", "Emerald Forest River", "10:12", R.drawable.vid_thumb_4, MediaCategory.VIDEOS),
        VideoMediaItem("vid_5", "Rocky Turquoise Coastline", "07:38", R.drawable.vid_thumb_5, MediaCategory.VIDEOS),
        VideoMediaItem("vid_6", "Illuminated Suspension Bridge", "14:27", R.drawable.vid_thumb_6, MediaCategory.VIDEOS),
        VideoMediaItem("vid_7", "Tropical Palm Sunset", "09:16", R.drawable.vid_thumb_7, MediaCategory.VIDEOS),
        VideoMediaItem("vid_8", "Misty Evergreen Forest", "11:03", R.drawable.vid_thumb_8, MediaCategory.VIDEOS),
        VideoMediaItem("vid_9", "Coastal Cliffs & Blue Water", "13:52", R.drawable.vid_thumb_9, MediaCategory.VIDEOS)
    )

    val playerAudioItems: List<VideoMediaItem> = listOf(
        VideoMediaItem("aud_1", "Alpine Breeze Acoustic", "03:42", R.drawable.vid_thumb_1, MediaCategory.AUDIO),
        VideoMediaItem("aud_2", "Sunset Waves Ambient", "04:15", R.drawable.vid_thumb_2, MediaCategory.AUDIO),
        VideoMediaItem("aud_3", "Midnight Synth Skyline", "03:58", R.drawable.vid_thumb_3, MediaCategory.AUDIO),
        VideoMediaItem("aud_4", "River Stream Meditation", "05:10", R.drawable.vid_thumb_4, MediaCategory.AUDIO),
        VideoMediaItem("aud_5", "Coastal Horizon Chimes", "02:54", R.drawable.vid_thumb_5, MediaCategory.AUDIO),
        VideoMediaItem("aud_6", "Harbor Bridge Nocturne", "04:33", R.drawable.vid_thumb_6, MediaCategory.AUDIO),
        VideoMediaItem("aud_7", "Island Twilight Acoustic", "03:21", R.drawable.vid_thumb_7, MediaCategory.AUDIO),
        VideoMediaItem("aud_8", "Pine Woods Rainfall", "06:05", R.drawable.vid_thumb_8, MediaCategory.AUDIO),
        VideoMediaItem("aud_9", "Ocean Cliff Resonance", "03:49", R.drawable.vid_thumb_9, MediaCategory.AUDIO)
    )

    val playerImageItems: List<VideoMediaItem> = listOf(
        VideoMediaItem("img_1", "Snowy Mountains Still", "RAW", R.drawable.vid_thumb_1, MediaCategory.IMAGES),
        VideoMediaItem("img_2", "Ocean Sunset Still", "RAW", R.drawable.vid_thumb_2, MediaCategory.IMAGES),
        VideoMediaItem("img_3", "City Skyline Still", "RAW", R.drawable.vid_thumb_3, MediaCategory.IMAGES),
        VideoMediaItem("img_4", "Forest River Still", "RAW", R.drawable.vid_thumb_4, MediaCategory.IMAGES),
        VideoMediaItem("img_5", "Turquoise Coast Still", "RAW", R.drawable.vid_thumb_5, MediaCategory.IMAGES),
        VideoMediaItem("img_6", "Suspension Bridge Still", "RAW", R.drawable.vid_thumb_6, MediaCategory.IMAGES),
        VideoMediaItem("img_7", "Tropical Sunset Still", "RAW", R.drawable.vid_thumb_7, MediaCategory.IMAGES),
        VideoMediaItem("img_8", "Green Forest Still", "RAW", R.drawable.vid_thumb_8, MediaCategory.IMAGES),
        VideoMediaItem("img_9", "Coastal Cliffs Still", "RAW", R.drawable.vid_thumb_9, MediaCategory.IMAGES)
    )

    val vaultDefaultItems: List<VaultMediaItem> = listOf(
        VaultMediaItem("vault_1", "Purple Mountain Landscape", R.drawable.vault_thumb_1, MediaCategory.VIDEOS),
        VaultMediaItem("vault_2", "Purple Lotus Flower", R.drawable.vault_thumb_2, MediaCategory.VIDEOS),
        VaultMediaItem("vault_3", "Colorful Abstract Flowing Waves", R.drawable.vault_thumb_3, MediaCategory.VIDEOS),
        VaultMediaItem("vault_4", "Green Aurora Over Snowy Landscape", R.drawable.vault_thumb_4, MediaCategory.VIDEOS),
        VaultMediaItem("vault_5", "Autumn Forest Reflected in Lake", R.drawable.vault_thumb_5, MediaCategory.VIDEOS),
        VaultMediaItem("vault_6", "Golden Ocean Horizon", R.drawable.vault_thumb_6, MediaCategory.VIDEOS),
        VaultMediaItem("vault_7", "Green Leaves With Water Droplets", R.drawable.vault_thumb_7, MediaCategory.VIDEOS),
        VaultMediaItem("vault_8", "Nighttime Illuminated City Skyline", R.drawable.vault_thumb_8, MediaCategory.VIDEOS),
        VaultMediaItem("vault_9", "Orange Desert Sand Dunes", R.drawable.vault_thumb_9, MediaCategory.VIDEOS)
    )
}
