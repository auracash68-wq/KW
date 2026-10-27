package com.example.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ksdownloader.KSAtlasPhotoItem
import com.example.ksdownloader.KSDownloadStatus
import com.example.ksdownloader.KSDownloadTaskState
import com.example.ksdownloader.KSDownloaderBridge
import com.example.ksdownloader.KSDownloaderWorksDetail
import com.example.ksdownloader.KSVideoQualityOption
import com.example.ksdownloader.KSWorksType
import com.example.model.AppDestination
import com.example.model.BulkContentType
import com.example.model.BulkItemStatus
import com.example.model.BulkPendingItem
import com.example.model.MediaCategory
import com.example.model.ReferenceDemoCatalog
import com.example.model.VaultMediaItem
import com.example.model.VideoMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KuaiUiState(
    val currentDestination: AppDestination = AppDestination.DOWNLOAD,
    val showBulkDownloadScreen: Boolean = false,
    val urlInput: String = "",
    val bulkDownloadEnabled: Boolean = true,
    val isDownloading: Boolean = false,
    val validationMessage: String? = null,
    val isValidationError: Boolean = false,
    val parsedPost: KSDownloaderWorksDetail? = null,
    val selectedParsedQuality: KSVideoQualityOption? = null,
    val activeDownloadTask: KSDownloadTaskState? = null,
    val bulkUrlInput: String = "",
    val bulkContentType: BulkContentType = BulkContentType.VIDEO,
    val bulkSelectedQuality: String = BulkContentType.VIDEO.defaultQuality,
    val bulkPendingItems: List<BulkPendingItem> = ReferenceDemoCatalog.initialBulkVideoItems,
    val bulkValidationMessage: String? = null,
    val isBulkValidationError: Boolean = false,
    val isBulkDownloadingAll: Boolean = false,
    val playerCategory: MediaCategory = MediaCategory.VIDEOS,
    val vaultCategory: MediaCategory = MediaCategory.VIDEOS,
    val activeVideoPreview: VideoMediaItem? = null,
    val activeVaultPreview: VaultMediaItem? = null,
    val isVaultUnlocked: Boolean = false,
    val vaultPinError: String? = null,
    val downloadedVideos: List<VideoMediaItem> = emptyList(),
    val downloadedAudios: List<VideoMediaItem> = emptyList(),
    val downloadedImages: List<VideoMediaItem> = emptyList(),
    val customVaultItems: List<VaultMediaItem> = emptyList()
) {
    val selectedBulkCount: Int
        get() = bulkPendingItems.count { it.isSelected }

    val displayedPlayerItems: List<VideoMediaItem>
        get() = when (playerCategory) {
            MediaCategory.VIDEOS -> (downloadedVideos + ReferenceDemoCatalog.playerVideoItems).take(9)
            MediaCategory.AUDIO -> (downloadedAudios + ReferenceDemoCatalog.playerAudioItems).take(9)
            MediaCategory.IMAGES -> (downloadedImages + ReferenceDemoCatalog.playerImageItems).take(9)
        }

    val displayedVaultItems: List<VaultMediaItem>
        get() {
            val userVaultForCategory = customVaultItems.filter { it.category == vaultCategory }
            val defaults = when (vaultCategory) {
                MediaCategory.VIDEOS -> ReferenceDemoCatalog.vaultDefaultItems
                MediaCategory.AUDIO -> ReferenceDemoCatalog.vaultDefaultItems.reversed()
                MediaCategory.IMAGES -> ReferenceDemoCatalog.vaultDefaultItems
            }
            return (userVaultForCategory + defaults).take(9).map {
                it.copy(isLocked = !isVaultUnlocked)
            }
        }
}

class KuaiViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KuaiUiState())
    val uiState: StateFlow<KuaiUiState> = _uiState.asStateFlow()

    init {
        KSDownloaderBridge.ensureNonLoginSessionCookies()
    }

    fun selectDestination(destination: AppDestination) {
        _uiState.update {
            it.copy(
                currentDestination = destination,
                showBulkDownloadScreen = if (destination == AppDestination.DOWNLOAD) false else it.showBulkDownloadScreen,
                validationMessage = null,
                bulkValidationMessage = null,
                activeVideoPreview = null,
                activeVaultPreview = null,
                vaultPinError = null
            )
        }
    }

    fun updateUrlInput(newUrl: String) {
        _uiState.update {
            it.copy(
                urlInput = newUrl,
                validationMessage = null,
                isValidationError = false
            )
        }
    }

    fun pasteUrlFromClipboard(clipboardText: String?) {
        val rawClipboard = clipboardText?.trim().orEmpty()
        if (rawClipboard.isEmpty()) {
            _uiState.update {
                it.copy(
                    validationMessage = "Clipboard is empty. Copy a Kuaishou or Kwai video link first.",
                    isValidationError = true
                )
            }
            return
        }

        val cleanedUrl = KSDownloaderBridge.cleanShareText(rawClipboard)
        if (cleanedUrl == null) {
            _uiState.update {
                it.copy(
                    validationMessage = "No valid Kuaishou or Kwai link found in clipboard.",
                    isValidationError = true
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                urlInput = cleanedUrl,
                validationMessage = "KS-Downloader extracting works metadata (direct connection)...",
                isValidationError = false
            )
        }

        viewModelScope.launch {
            extractWithKSDownloader(rawClipboard, autoStartDefaultDownload = false)
        }
    }

    fun toggleBulkDownload(enabled: Boolean) {
        _uiState.update { it.copy(bulkDownloadEnabled = enabled) }
    }

    fun onBulkDownloadClick() {
        _uiState.update { state ->
            if (state.showBulkDownloadScreen) {
                state
            } else {
                state.copy(
                    currentDestination = AppDestination.DOWNLOAD,
                    showBulkDownloadScreen = true,
                    validationMessage = null,
                    bulkValidationMessage = null,
                    isBulkValidationError = false
                )
            }
        }
    }

    fun closeBulkDownloadScreen() {
        _uiState.update {
            it.copy(
                showBulkDownloadScreen = false,
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun updateBulkUrlInput(newUrl: String) {
        _uiState.update {
            it.copy(
                bulkUrlInput = newUrl,
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun addBulkUrl() {
        val rawInput = _uiState.value.bulkUrlInput.trim()
        if (rawInput.isEmpty()) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "Please enter or paste a video URL first.",
                    isBulkValidationError = true
                )
            }
            return
        }

        // Support space-separated batch links from KS-Downloader
        val extractedUrls = KSDownloaderBridge.extractBatchUrls(rawInput).ifEmpty {
            listOf(rawInput)
        }

        val validUrls = extractedUrls.filter { url ->
            (url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)) &&
                (Patterns.WEB_URL.matcher(url).matches() || url.contains("."))
        }

        if (validUrls.isEmpty()) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "Please enter a valid http:// or https:// URL.",
                    isBulkValidationError = true
                )
            }
            return
        }

        val state = _uiState.value
        val newUrls = validUrls.filterNot { candidate ->
            state.bulkPendingItems.any { it.url.equals(candidate, ignoreCase = true) }
        }

        if (newUrls.isEmpty()) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "This URL is already in the pending list.",
                    isBulkValidationError = true
                )
            }
            return
        }

        val thumbPool = listOf(
            R.drawable.vid_thumb_1,
            R.drawable.vid_thumb_2,
            R.drawable.vid_thumb_3,
            R.drawable.vid_thumb_4,
            R.drawable.vid_thumb_5,
            R.drawable.vid_thumb_6
        )

        val addedItems = newUrls.mapIndexed { offset, cleanedUrl ->
            val nextIndex = state.bulkPendingItems.size + offset + 1
            val thumbRes = thumbPool[(nextIndex - 1) % thumbPool.size]
            val (shareTitle, _) = KSDownloaderBridge.extractShareTextMetadata(rawInput, cleanedUrl)
            val (worksId, _) = KSDownloaderBridge.extractWorksAndAuthorIds(cleanedUrl)

            val resolvedTitle = if (newUrls.size == 1 && !shareTitle.isNullOrBlank()) {
                shareTitle
            } else if (worksId.isNotBlank()) {
                "KS Works ($worksId)"
            } else {
                "${state.bulkContentType.label} Stream #$nextIndex"
            }

            BulkPendingItem(
                id = "bulk_${System.currentTimeMillis()}_$nextIndex",
                title = resolvedTitle,
                url = cleanedUrl,
                fileSize = when (state.bulkContentType) {
                    BulkContentType.VIDEO -> "38.4 MB"
                    BulkContentType.AUDIO -> "5.4 MB"
                    BulkContentType.PHOTO -> "3.1 MB"
                },
                duration = when (state.bulkContentType) {
                    BulkContentType.VIDEO -> "02:06"
                    BulkContentType.AUDIO -> "02:06"
                    BulkContentType.PHOTO -> "ATLAS"
                },
                quality = state.bulkSelectedQuality,
                contentType = state.bulkContentType,
                thumbnailRes = thumbRes,
                isSelected = true,
                status = BulkItemStatus.PENDING
            )
        }

        _uiState.update {
            it.copy(
                bulkUrlInput = "",
                bulkPendingItems = it.bulkPendingItems + addedItems,
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun selectBulkContentType(contentType: BulkContentType) {
        _uiState.update { state ->
            val newQuality = contentType.defaultQuality
            state.copy(
                bulkContentType = contentType,
                bulkSelectedQuality = newQuality,
                bulkPendingItems = state.bulkPendingItems.map { item ->
                    item.copy(
                        contentType = contentType,
                        quality = newQuality
                    )
                },
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun selectBulkQuality(quality: String) {
        _uiState.update { state ->
            state.copy(
                bulkSelectedQuality = quality,
                bulkPendingItems = state.bulkPendingItems.map { item ->
                    if (item.isSelected) item.copy(quality = quality) else item
                },
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun toggleBulkItemSelection(itemId: String) {
        _uiState.update { state ->
            state.copy(
                bulkPendingItems = state.bulkPendingItems.map { item ->
                    if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
                },
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun clearAllBulkItems() {
        _uiState.update {
            it.copy(
                bulkPendingItems = emptyList(),
                isBulkDownloadingAll = false,
                bulkValidationMessage = null,
                isBulkValidationError = false
            )
        }
    }

    fun startBulkDownloadAll() {
        val currentItems = _uiState.value.bulkPendingItems
        val selectedIds = currentItems.filter { it.isSelected }.map { it.id }

        if (selectedIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "Please select at least one item to download.",
                    isBulkValidationError = true
                )
            }
            return
        }

        if (_uiState.value.isBulkDownloadingAll) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBulkDownloadingAll = true,
                    bulkValidationMessage = "KS-Downloader processing ${selectedIds.size} selected item(s)...",
                    isBulkValidationError = false
                )
            }

            var completedCount = 0
            var lastErrorText: String? = null

            for (targetId in selectedIds) {
                val currentItem = _uiState.value.bulkPendingItems.firstOrNull { it.id == targetId } ?: continue

                _uiState.update { state ->
                    state.copy(
                        bulkPendingItems = state.bulkPendingItems.map { item ->
                            if (item.id == targetId) {
                                item.copy(status = BulkItemStatus.DOWNLOADING, progressPercent = 5)
                            } else {
                                item
                            }
                        }
                    )
                }

                val category = when (currentItem.contentType) {
                    BulkContentType.VIDEO -> MediaCategory.VIDEOS
                    BulkContentType.AUDIO -> MediaCategory.AUDIO
                    BulkContentType.PHOTO -> MediaCategory.IMAGES
                }
                val ext = when (currentItem.contentType) {
                    BulkContentType.VIDEO -> "mp4"
                    BulkContentType.AUDIO -> "mp3"
                    BulkContentType.PHOTO -> "jpg"
                }

                val extractRes = KSDownloaderBridge.extractWorks(currentItem.url, simulateNetworkDelayMs = 0L)
                extractRes.fold(
                    onSuccess = { works ->
                        val streamUrl = when (currentItem.contentType) {
                            BulkContentType.VIDEO -> works.videoQualities.firstOrNull()?.videoUrl ?: works.downloadUrls.firstOrNull()
                            BulkContentType.AUDIO -> works.musicUrl.ifBlank { works.videoQualities.firstOrNull()?.videoUrl }
                            BulkContentType.PHOTO -> works.photoAtlas.firstOrNull()?.imageUrl ?: works.coverUrl
                        }.orEmpty()

                        val cleanName = works.buildKSFileName(extension = ext, qualityTag = currentItem.quality)
                        val dlRes = KSDownloaderBridge.streamDownloadToDevice(
                            worksId = works.worksId,
                            fileName = cleanName,
                            targetLabel = works.description,
                            sourceUrl = streamUrl,
                            totalBytes = 0L,
                            mediaCategory = category,
                            stepDelayMs = 0L,
                            onProgress = { prog ->
                                _uiState.update { state ->
                                    state.copy(
                                        bulkPendingItems = state.bulkPendingItems.map { item ->
                                            if (item.id == targetId) {
                                                item.copy(
                                                    status = BulkItemStatus.DOWNLOADING,
                                                    progressPercent = prog.progressPercent
                                                )
                                            } else item
                                        }
                                    )
                                }
                            }
                        )

                        dlRes.fold(
                            onSuccess = { finishedTask ->
                                completedCount++
                                val realSize = KSDownloaderBridge.formatBytes(finishedTask.bytesDownloaded)
                                val queuedItem = VideoMediaItem(
                                    id = "ks_bulk_dl_${currentItem.id}",
                                    title = works.description,
                                    duration = works.durationText,
                                    thumbnailRes = currentItem.thumbnailRes,
                                    category = category,
                                    isDemonstration = false,
                                    sourceUrl = streamUrl,
                                    fileName = cleanName,
                                    qualityLabel = currentItem.quality,
                                    fileSizeText = realSize,
                                    localFilePath = finishedTask.savedFilePath,
                                    authorName = works.authorNickname
                                )
                                _uiState.update { state ->
                                    state.copy(
                                        downloadedVideos = if (category == MediaCategory.VIDEOS) {
                                            listOf(queuedItem) + state.downloadedVideos.filterNot { it.id == queuedItem.id }
                                        } else state.downloadedVideos,
                                        downloadedAudios = if (category == MediaCategory.AUDIO) {
                                            listOf(queuedItem) + state.downloadedAudios.filterNot { it.id == queuedItem.id }
                                        } else state.downloadedAudios,
                                        downloadedImages = if (category == MediaCategory.IMAGES) {
                                            listOf(queuedItem) + state.downloadedImages.filterNot { it.id == queuedItem.id }
                                        } else state.downloadedImages,
                                        bulkPendingItems = state.bulkPendingItems.map { item ->
                                            if (item.id == targetId) {
                                                item.copy(
                                                    title = works.description,
                                                    fileSize = realSize,
                                                    status = BulkItemStatus.COMPLETED,
                                                    progressPercent = 100
                                                )
                                            } else item
                                        }
                                    )
                                }
                            },
                            onFailure = { dlErr ->
                                lastErrorText = dlErr.message
                                _uiState.update { state ->
                                    state.copy(
                                        bulkPendingItems = state.bulkPendingItems.map { item ->
                                            if (item.id == targetId) {
                                                item.copy(status = BulkItemStatus.FAILED, progressPercent = 0)
                                            } else item
                                        }
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { extErr ->
                        lastErrorText = extErr.message
                        _uiState.update { state ->
                            state.copy(
                                bulkPendingItems = state.bulkPendingItems.map { item ->
                                    if (item.id == targetId) {
                                        item.copy(status = BulkItemStatus.FAILED, progressPercent = 0)
                                    } else item
                                }
                            )
                        }
                    }
                )
            }

            _uiState.update {
                if (completedCount > 0) {
                    it.copy(
                        isBulkDownloadingAll = false,
                        bulkValidationMessage = "Saved $completedCount real media file(s) to Downloads/KuaiVideoSave & Player.",
                        isBulkValidationError = false
                    )
                } else {
                    it.copy(
                        isBulkDownloadingAll = false,
                        bulkValidationMessage = lastErrorText ?: "Failed to extract real media streams for selected URLs.",
                        isBulkValidationError = true
                    )
                }
            }
        }
    }

    fun startDownload() {
        val rawInput = _uiState.value.urlInput.trim()
        if (rawInput.isEmpty()) {
            _uiState.update {
                it.copy(
                    validationMessage = "Please enter or paste a video URL first.",
                    isValidationError = true
                )
            }
            return
        }

        val cleanedUrl = KSDownloaderBridge.cleanShareText(rawInput)
        val isValidHttpUrl = cleanedUrl != null &&
            (cleanedUrl.startsWith("http://", ignoreCase = true) ||
                cleanedUrl.startsWith("https://", ignoreCase = true)) &&
            (Patterns.WEB_URL.matcher(cleanedUrl).matches() || cleanedUrl.contains("."))

        if (!isValidHttpUrl || cleanedUrl == null) {
            _uiState.update {
                it.copy(
                    validationMessage = "Please enter a valid Kuaishou, Kwai, or http(s):// video URL.",
                    isValidationError = true
                )
            }
            return
        }

        // Immediately update the input box to show ONLY the clean URL
        _uiState.update {
            it.copy(
                urlInput = cleanedUrl,
                validationMessage = null,
                isValidationError = false
            )
        }

        viewModelScope.launch {
            extractWithKSDownloader(rawInput, autoStartDefaultDownload = true)
        }
    }

    private suspend fun extractWithKSDownloader(
        rawInput: String,
        autoStartDefaultDownload: Boolean
    ) {
        val cleanDisplayUrl = KSDownloaderBridge.cleanShareText(rawInput)
        _uiState.update {
            it.copy(
                isDownloading = true,
                urlInput = cleanDisplayUrl ?: it.urlInput,
                validationMessage = "KS-Downloader extracting works metadata (direct connection)...",
                isValidationError = false
            )
        }

        val extractResult = KSDownloaderBridge.extractWorks(rawInput, simulateNetworkDelayMs = 0L)
        extractResult.fold(
            onSuccess = { worksDetail ->
                val defaultQuality = worksDetail.videoQualities.firstOrNull {
                    it.shortBadge.equals("1080p", ignoreCase = true)
                } ?: worksDetail.videoQualities.firstOrNull()

                // If user pasted space-separated batch links, queue the extra links into Bulk Download list automatically
                if (worksDetail.batchSiblingUrls.isNotEmpty()) {
                    val existingUrls = _uiState.value.bulkPendingItems.map { it.url.lowercase() }.toSet()
                    val extraItems = worksDetail.batchSiblingUrls
                        .filterNot { existingUrls.contains(it.lowercase()) }
                        .mapIndexed { idx, siblingUrl ->
                            val (sibWorksId, _) = KSDownloaderBridge.extractWorksAndAuthorIds(siblingUrl)
                            BulkPendingItem(
                                id = "ks_batch_${System.currentTimeMillis()}_$idx",
                                title = "KS Batch Works ($sibWorksId)",
                                url = siblingUrl,
                                fileSize = "HD Stream",
                                duration = "HD",
                                quality = "1080p",
                                contentType = BulkContentType.VIDEO,
                                thumbnailRes = R.drawable.vid_thumb_2,
                                isSelected = true
                            )
                        }
                    if (extraItems.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(bulkPendingItems = state.bulkPendingItems + extraItems)
                        }
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        isDownloading = false,
                        urlInput = if (autoStartDefaultDownload) "" else worksDetail.worksUrl,
                        parsedPost = worksDetail,
                        selectedParsedQuality = defaultQuality,
                        validationMessage = if (worksDetail.worksType == KSWorksType.PHOTO_ATLAS) {
                            "KS-Downloader • 作品ID:${worksDetail.worksId} (${worksDetail.photoAtlas.size} HD Photos)"
                        } else {
                            "KS-Downloader • 作品ID:${worksDetail.worksId} (@${worksDetail.authorNickname}) • Select quality or action."
                        },
                        isValidationError = false
                    )
                }

                if (autoStartDefaultDownload) {
                    if (worksDetail.worksType == KSWorksType.PHOTO_ATLAS) {
                        downloadAllParsedPhotos()
                    } else {
                        downloadParsedVideoNoWatermark()
                    }
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        validationMessage = err.message ?: "Unable to extract Kuaishou works.",
                        isValidationError = true
                    )
                }
            }
        )
    }

    fun selectParsedVideoQuality(quality: KSVideoQualityOption) {
        _uiState.update {
            it.copy(
                selectedParsedQuality = quality,
                validationMessage = "Selected ${quality.qualityLabel} (${quality.formattedSize}, No Watermark)",
                isValidationError = false
            )
        }
    }

    fun downloadParsedVideoNoWatermark() {
        val post = _uiState.value.parsedPost ?: return
        val quality = _uiState.value.selectedParsedQuality
            ?: post.videoQualities.firstOrNull()
            ?: return

        val fileName = post.buildKSFileName(extension = "mp4", qualityTag = quality.shortBadge)
        viewModelScope.launch {
            executeKSDownloadStream(
                worksId = post.worksId,
                fileName = fileName,
                targetLabel = "${post.description} [${quality.shortBadge}]",
                sourceUrl = quality.videoUrl,
                totalBytes = quality.fileSizeBytes,
                durationText = post.durationText,
                formattedSize = quality.formattedSize,
                qualityLabel = quality.qualityLabel,
                authorName = post.authorNickname,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.VIDEOS
            )
        }
    }

    fun downloadParsedOriginalAudio() {
        val post = _uiState.value.parsedPost ?: return
        val audio = post.musicStream
        val fileName = post.buildKSFileName(extension = audio.extension, qualityTag = "Music_${audio.bitrateKbps}k")
        viewModelScope.launch {
            executeKSDownloadStream(
                worksId = post.worksId,
                fileName = fileName,
                targetLabel = "${post.description} (Original MP3)",
                sourceUrl = audio.musicUrl,
                totalBytes = audio.fileSizeBytes,
                durationText = audio.durationText,
                formattedSize = audio.formattedSize,
                qualityLabel = "Original MP3 (${audio.bitrateKbps}kbps)",
                authorName = post.authorNickname,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.AUDIO
            )
        }
    }

    fun downloadParsedCoverThumbnail() {
        val post = _uiState.value.parsedPost ?: return
        val fileName = post.buildKSFileName(extension = "jpg", qualityTag = "CoverHD")
        viewModelScope.launch {
            executeKSDownloadStream(
                worksId = post.worksId,
                fileName = fileName,
                targetLabel = "${post.description} (HD Cover)",
                sourceUrl = post.coverUrl,
                totalBytes = post.coverFileSizeBytes,
                durationText = "HD Cover",
                formattedSize = post.coverFormattedSize,
                qualityLabel = "HD Cover Thumbnail",
                authorName = post.authorNickname,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.IMAGES
            )
        }
    }

    fun downloadSingleParsedPhoto(photo: KSAtlasPhotoItem) {
        val post = _uiState.value.parsedPost ?: return
        val fileName = post.buildKSFileName(extension = "jpg", qualityTag = "Atlas_${photo.index}")
        viewModelScope.launch {
            executeKSDownloadStream(
                worksId = post.worksId,
                fileName = fileName,
                targetLabel = "${post.description} - Photo #${photo.index}",
                sourceUrl = photo.imageUrl,
                totalBytes = photo.fileSizeBytes,
                durationText = "HD",
                formattedSize = photo.formattedSize,
                qualityLabel = "HD Atlas Photo (${photo.resolution})",
                authorName = post.authorNickname,
                thumbnailRes = photo.thumbnailRes,
                mediaCategory = MediaCategory.IMAGES
            )
        }
    }

    fun downloadAllParsedPhotos() {
        val post = _uiState.value.parsedPost ?: return
        if (post.photoAtlas.isEmpty()) return

        viewModelScope.launch {
            for (photo in post.photoAtlas) {
                val fileName = post.buildKSFileName(extension = "jpg", qualityTag = "Atlas_${photo.index}")
                executeKSDownloadStream(
                    worksId = post.worksId,
                    fileName = fileName,
                    targetLabel = "${post.description} - Photo #${photo.index}",
                    sourceUrl = photo.imageUrl,
                    totalBytes = photo.fileSizeBytes,
                    durationText = "HD",
                    formattedSize = photo.formattedSize,
                    qualityLabel = "HD Atlas Photo (${photo.resolution})",
                    authorName = post.authorNickname,
                    thumbnailRes = photo.thumbnailRes,
                    mediaCategory = MediaCategory.IMAGES
                )
            }
        }
    }

    private suspend fun executeKSDownloadStream(
        worksId: String,
        fileName: String,
        targetLabel: String,
        sourceUrl: String,
        totalBytes: Long,
        durationText: String,
        formattedSize: String,
        qualityLabel: String,
        authorName: String,
        thumbnailRes: Int,
        mediaCategory: MediaCategory
    ) {
        val result = KSDownloaderBridge.streamDownloadToDevice(
            worksId = worksId,
            fileName = fileName,
            targetLabel = targetLabel,
            sourceUrl = sourceUrl,
            totalBytes = totalBytes,
            mediaCategory = mediaCategory,
            stepDelayMs = 0L,
            onProgress = { progressState ->
                _uiState.update { it.copy(activeDownloadTask = progressState) }
            }
        )

        result.fold(
            onSuccess = { finishedTask ->
                val actualSizeText = KSDownloaderBridge.formatBytes(finishedTask.bytesDownloaded)
                val savedItem = VideoMediaItem(
                    id = "ks_dl_${System.currentTimeMillis()}",
                    title = targetLabel,
                    duration = durationText,
                    thumbnailRes = thumbnailRes,
                    category = mediaCategory,
                    isDemonstration = false,
                    sourceUrl = sourceUrl,
                    fileName = fileName,
                    qualityLabel = qualityLabel,
                    fileSizeText = actualSizeText,
                    localFilePath = finishedTask.savedFilePath,
                    authorName = authorName
                )

                _uiState.update { state ->
                    state.copy(
                        downloadedVideos = if (mediaCategory == MediaCategory.VIDEOS) {
                            listOf(savedItem) + state.downloadedVideos
                        } else state.downloadedVideos,
                        downloadedAudios = if (mediaCategory == MediaCategory.AUDIO) {
                            listOf(savedItem) + state.downloadedAudios
                        } else state.downloadedAudios,
                        downloadedImages = if (mediaCategory == MediaCategory.IMAGES) {
                            listOf(savedItem) + state.downloadedImages
                        } else state.downloadedImages,
                        validationMessage = "Saved $fileName ($actualSizeText) to Downloads/KuaiVideoSave & Player.",
                        isValidationError = false
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        validationMessage = "Binary stream download error: ${err.message ?: "Failed to read stream from CDN"}",
                        isValidationError = true
                    )
                }
            }
        )
    }

    fun togglePauseResumeDownload() {
        val current = _uiState.value.activeDownloadTask ?: return
        if (current.status == KSDownloadStatus.PAUSED) {
            KSDownloaderBridge.resumeActiveDownload()
        } else if (current.status == KSDownloadStatus.DOWNLOADING) {
            KSDownloaderBridge.pauseActiveDownload()
        }
    }

    fun cancelActiveDownload() {
        KSDownloaderBridge.cancelActiveDownload()
        _uiState.update {
            it.copy(
                validationMessage = "Download cancelled.",
                isValidationError = true
            )
        }
    }

    fun moveToPrivateVault(item: VideoMediaItem) {
        val vaultItem = VaultMediaItem(
            id = "vault_${item.id}",
            title = item.title,
            thumbnailRes = item.thumbnailRes,
            category = item.category,
            isLocked = !_uiState.value.isVaultUnlocked,
            isDemonstration = false,
            duration = item.duration,
            sourceUrl = item.sourceUrl,
            fileName = item.fileName,
            qualityLabel = item.qualityLabel,
            fileSizeText = item.fileSizeText,
            localFilePath = item.localFilePath
        )

        _uiState.update { state ->
            state.copy(
                customVaultItems = listOf(vaultItem) + state.customVaultItems.filterNot { it.id == vaultItem.id },
                downloadedVideos = state.downloadedVideos.filterNot { it.id == item.id },
                downloadedAudios = state.downloadedAudios.filterNot { it.id == item.id },
                downloadedImages = state.downloadedImages.filterNot { it.id == item.id },
                activeVideoPreview = null,
                vaultCategory = item.category
            )
        }
    }

    fun unlockVaultWithPin(pin: String) {
        if (pin.trim() == "1234" || pin.trim().length == 4) {
            _uiState.update { state ->
                state.copy(
                    isVaultUnlocked = true,
                    vaultPinError = null,
                    activeVaultPreview = state.activeVaultPreview?.copy(isLocked = false)
                )
            }
        } else {
            _uiState.update {
                it.copy(vaultPinError = "Enter a 4-digit Vault PIN (Default: 1234)")
            }
        }
    }

    fun selectPlayerCategory(category: MediaCategory) {
        _uiState.update { it.copy(playerCategory = category) }
    }

    fun selectVaultCategory(category: MediaCategory) {
        _uiState.update { it.copy(vaultCategory = category) }
    }

    fun openVideoPreview(item: VideoMediaItem) {
        _uiState.update { it.copy(activeVideoPreview = item) }
    }

    fun closeVideoPreview() {
        _uiState.update { it.copy(activeVideoPreview = null) }
    }

    fun openVaultPreview(item: VaultMediaItem) {
        _uiState.update { it.copy(activeVaultPreview = item, vaultPinError = null) }
    }

    fun closeVaultPreview() {
        _uiState.update { it.copy(activeVaultPreview = null, vaultPinError = null) }
    }
}
