package com.example.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.engine.DownloadTaskState
import com.example.engine.DownloadTaskStatus
import com.example.engine.KuaishouEngineService
import com.example.engine.KuaishouParsedPost
import com.example.engine.KuaishouPhotoItem
import com.example.engine.KuaishouPostType
import com.example.engine.KuaishouVideoQualityStream
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
    val parsedPost: KuaishouParsedPost? = null,
    val selectedParsedQuality: KuaishouVideoQualityStream? = null,
    val activeDownloadTask: DownloadTaskState? = null,
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
        viewModelScope.launch {
            KuaishouEngineService.syncRemoteRulesIfNeeded()
        }
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

        val cleanedUrl = KuaishouEngineService.cleanShareText(rawClipboard) ?: rawClipboard
        _uiState.update {
            it.copy(
                urlInput = cleanedUrl,
                validationMessage = "Link cleaned from clipboard. Parsing Kuaishou media streams...",
                isValidationError = false
            )
        }

        viewModelScope.launch {
            parseInputWithEngine(rawClipboard, autoStartDefaultDownload = false)
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

        val cleanedUrl = KuaishouEngineService.cleanShareText(rawInput) ?: rawInput
        val isValidHttpUrl = (cleanedUrl.startsWith("http://", ignoreCase = true) ||
            cleanedUrl.startsWith("https://", ignoreCase = true)) &&
            (Patterns.WEB_URL.matcher(cleanedUrl).matches() || cleanedUrl.contains("."))

        if (!isValidHttpUrl) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "Please enter a valid http:// or https:// URL.",
                    isBulkValidationError = true
                )
            }
            return
        }

        val existing = _uiState.value.bulkPendingItems.any {
            it.url.equals(cleanedUrl, ignoreCase = true)
        }
        if (existing) {
            _uiState.update {
                it.copy(
                    bulkValidationMessage = "This URL is already in the pending list.",
                    isBulkValidationError = true
                )
            }
            return
        }

        val state = _uiState.value
        val thumbPool = listOf(
            R.drawable.vid_thumb_1,
            R.drawable.vid_thumb_2,
            R.drawable.vid_thumb_3,
            R.drawable.vid_thumb_4,
            R.drawable.vid_thumb_5,
            R.drawable.vid_thumb_6
        )
        val nextIndex = state.bulkPendingItems.size + 1
        val thumbRes = thumbPool[(nextIndex - 1) % thumbPool.size]
        val (shareTitle, _) = KuaishouEngineService.extractShareTextMetadata(rawInput, cleanedUrl)
        val cleanSlug = cleanedUrl
            .substringAfter("://")
            .substringAfterLast('/')
            .substringBefore('?')
            .ifBlank { "${state.bulkContentType.label} Item #$nextIndex" }

        val resolvedTitle = shareTitle
            ?: if (cleanSlug.length > 4) "Media Stream ($cleanSlug)" else "${state.bulkContentType.label} Stream #$nextIndex"

        val newItem = BulkPendingItem(
            id = "bulk_${System.currentTimeMillis()}_$nextIndex",
            title = resolvedTitle,
            url = cleanedUrl,
            fileSize = when (state.bulkContentType) {
                BulkContentType.VIDEO -> "38.4 MB"
                BulkContentType.AUDIO -> "6.8 MB"
                BulkContentType.PHOTO -> "3.2 MB"
            },
            duration = when (state.bulkContentType) {
                BulkContentType.VIDEO -> "06:24"
                BulkContentType.AUDIO -> "03:45"
                BulkContentType.PHOTO -> "IMG"
            },
            quality = state.bulkSelectedQuality,
            contentType = state.bulkContentType,
            thumbnailRes = thumbRes,
            isSelected = true,
            status = BulkItemStatus.PENDING
        )

        _uiState.update {
            it.copy(
                bulkUrlInput = "",
                bulkPendingItems = it.bulkPendingItems + newItem,
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
                    bulkValidationMessage = "Processing ${selectedIds.size} selected item(s) sequentially...",
                    isBulkValidationError = false
                )
            }

            var completedCount = 0
            for (targetId in selectedIds) {
                val itemExists = _uiState.value.bulkPendingItems.any { it.id == targetId }
                if (!itemExists) continue

                _uiState.update { state ->
                    state.copy(
                        bulkPendingItems = state.bulkPendingItems.map { item ->
                            if (item.id == targetId) {
                                item.copy(status = BulkItemStatus.DOWNLOADING, progressPercent = 45)
                            } else {
                                item
                            }
                        }
                    )
                }
                delay(180)

                val currentItem = _uiState.value.bulkPendingItems.firstOrNull { it.id == targetId }
                if (currentItem != null) {
                    completedCount++
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
                    val cleanName = "KuaiSave_Bulk_${currentItem.id.takeLast(6)}_${currentItem.quality}.$ext"
                    val queuedItem = VideoMediaItem(
                        id = "bulk_dl_${currentItem.id}",
                        title = currentItem.title,
                        duration = currentItem.duration,
                        thumbnailRes = currentItem.thumbnailRes,
                        category = category,
                        isDemonstration = false,
                        sourceUrl = currentItem.url,
                        fileName = cleanName,
                        qualityLabel = currentItem.quality,
                        fileSizeText = currentItem.fileSize
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
                                    item.copy(status = BulkItemStatus.COMPLETED, progressPercent = 100)
                                } else {
                                    item
                                }
                            }
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isBulkDownloadingAll = false,
                    bulkValidationMessage = "Processed $completedCount item(s) and queued in Player.",
                    isBulkValidationError = false
                )
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

        val cleanedUrl = KuaishouEngineService.cleanShareText(rawInput)
        val isValidHttpUrl = cleanedUrl != null &&
            (cleanedUrl.startsWith("http://", ignoreCase = true) ||
                cleanedUrl.startsWith("https://", ignoreCase = true)) &&
            (Patterns.WEB_URL.matcher(cleanedUrl).matches() || cleanedUrl.contains("."))

        if (!isValidHttpUrl) {
            _uiState.update {
                it.copy(
                    validationMessage = "Please enter a valid Kuaishou, Kwai, or http(s):// video URL.",
                    isValidationError = true
                )
            }
            return
        }

        viewModelScope.launch {
            parseInputWithEngine(rawInput, autoStartDefaultDownload = true)
        }
    }

    private suspend fun parseInputWithEngine(
        rawInput: String,
        autoStartDefaultDownload: Boolean
    ) {
        _uiState.update {
            it.copy(
                isDownloading = true,
                validationMessage = "Parsing Kuaishou/Kwai stream (zero-login session)...",
                isValidationError = false
            )
        }

        val parseResult = KuaishouEngineService.parseUrl(rawInput, simulateNetworkDelayMs = 150L)
        parseResult.fold(
            onSuccess = { parsedPost ->
                val defaultQuality = parsedPost.availableQualities.firstOrNull {
                    it.shortBadge.equals("1080p", ignoreCase = true)
                } ?: parsedPost.availableQualities.firstOrNull()

                _uiState.update { state ->
                    state.copy(
                        isDownloading = false,
                        urlInput = if (autoStartDefaultDownload) "" else parsedPost.cleanShareUrl,
                        parsedPost = parsedPost,
                        selectedParsedQuality = defaultQuality,
                        validationMessage = if (parsedPost.postType == KuaishouPostType.PHOTO_CAROUSEL) {
                            "Parsed Photo Carousel (${parsedPost.photoCarousel.size} HD photos) • Select action below."
                        } else {
                            "Parsed Watermark-Free Stream (@${parsedPost.authorName}) • Select quality or action."
                        },
                        isValidationError = false
                    )
                }

                if (autoStartDefaultDownload) {
                    if (parsedPost.postType == KuaishouPostType.PHOTO_CAROUSEL) {
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
                        validationMessage = err.message ?: "Unable to parse media URL.",
                        isValidationError = true
                    )
                }
            }
        )
    }

    fun selectParsedVideoQuality(quality: KuaishouVideoQualityStream) {
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
            ?: post.availableQualities.firstOrNull()
            ?: return

        val fileName = post.buildCleanFileName(extension = "mp4", qualityTag = quality.shortBadge)
        viewModelScope.launch {
            executeStreamingDownload(
                fileName = fileName,
                targetLabel = "${post.title} [${quality.shortBadge}]",
                sourceUrl = quality.videoUrl,
                totalBytes = quality.fileSizeBytes,
                durationText = post.durationText,
                formattedSize = quality.formattedSize,
                qualityLabel = quality.qualityLabel,
                authorName = post.authorName,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.VIDEOS
            )
        }
    }

    fun downloadParsedOriginalAudio() {
        val post = _uiState.value.parsedPost ?: return
        val audio = post.audioStream
        val fileName = post.buildCleanFileName(extension = "mp3", qualityTag = "${audio.bitrateKbps}k")
        viewModelScope.launch {
            executeStreamingDownload(
                fileName = fileName,
                targetLabel = "${post.title} (Original MP3)",
                sourceUrl = audio.audioUrl,
                totalBytes = audio.fileSizeBytes,
                durationText = audio.durationText,
                formattedSize = audio.formattedSize,
                qualityLabel = "Original MP3 (${audio.bitrateKbps}kbps)",
                authorName = post.authorName,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.AUDIO
            )
        }
    }

    fun downloadParsedCoverThumbnail() {
        val post = _uiState.value.parsedPost ?: return
        val fileName = post.buildCleanFileName(extension = "jpg", qualityTag = "CoverHD")
        viewModelScope.launch {
            executeStreamingDownload(
                fileName = fileName,
                targetLabel = "${post.title} (HD Cover)",
                sourceUrl = post.coverUrl,
                totalBytes = post.coverFileSizeBytes,
                durationText = "HD JPG",
                formattedSize = post.coverFormattedSize,
                qualityLabel = "HD Cover Thumbnail",
                authorName = post.authorName,
                thumbnailRes = post.thumbnailRes,
                mediaCategory = MediaCategory.IMAGES
            )
        }
    }

    fun downloadSingleParsedPhoto(photo: KuaishouPhotoItem) {
        val post = _uiState.value.parsedPost ?: return
        val fileName = post.buildCleanFileName(extension = "jpg", qualityTag = "Photo${photo.index}HD")
        viewModelScope.launch {
            executeStreamingDownload(
                fileName = fileName,
                targetLabel = "${post.title} - Photo #${photo.index}",
                sourceUrl = photo.imageUrl,
                totalBytes = photo.fileSizeBytes,
                durationText = "HD",
                formattedSize = photo.formattedSize,
                qualityLabel = "HD Photo (${photo.resolution})",
                authorName = post.authorName,
                thumbnailRes = photo.thumbnailRes,
                mediaCategory = MediaCategory.IMAGES
            )
        }
    }

    fun downloadAllParsedPhotos() {
        val post = _uiState.value.parsedPost ?: return
        if (post.photoCarousel.isEmpty()) return

        viewModelScope.launch {
            for (photo in post.photoCarousel) {
                val fileName = post.buildCleanFileName(extension = "jpg", qualityTag = "Photo${photo.index}HD")
                executeStreamingDownload(
                    fileName = fileName,
                    targetLabel = "${post.title} - Photo #${photo.index}",
                    sourceUrl = photo.imageUrl,
                    totalBytes = photo.fileSizeBytes,
                    durationText = "HD",
                    formattedSize = photo.formattedSize,
                    qualityLabel = "HD Photo (${photo.resolution})",
                    authorName = post.authorName,
                    thumbnailRes = photo.thumbnailRes,
                    mediaCategory = MediaCategory.IMAGES
                )
            }
            _uiState.update {
                it.copy(
                    validationMessage = "Downloaded all ${post.photoCarousel.size} HD photos into Offline Library.",
                    isValidationError = false
                )
            }
        }
    }

    private suspend fun executeStreamingDownload(
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
        val result = KuaishouEngineService.streamDownloadToDevice(
            fileName = fileName,
            targetLabel = targetLabel,
            sourceUrl = sourceUrl,
            totalBytes = totalBytes,
            mediaCategory = mediaCategory,
            stepDelayMs = 25L,
            onProgress = { progressState ->
                _uiState.update { it.copy(activeDownloadTask = progressState) }
            }
        )

        result.onSuccess { finishedTask ->
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
                fileSizeText = formattedSize,
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
                    validationMessage = "Saved $fileName to Offline Library (Player).",
                    isValidationError = false
                )
            }
        }
    }

    fun togglePauseResumeDownload() {
        val current = _uiState.value.activeDownloadTask ?: return
        if (current.status == DownloadTaskStatus.PAUSED) {
            KuaishouEngineService.resumeActiveDownload()
        } else if (current.status == DownloadTaskStatus.DOWNLOADING) {
            KuaishouEngineService.pauseActiveDownload()
        }
    }

    fun cancelActiveDownload() {
        KuaishouEngineService.cancelActiveDownload()
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
