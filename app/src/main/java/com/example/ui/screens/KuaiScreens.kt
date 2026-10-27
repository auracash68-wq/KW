package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.DownloadTaskState
import com.example.engine.KuaishouParsedPost
import com.example.engine.KuaishouPhotoItem
import com.example.engine.KuaishouVideoQualityStream
import com.example.model.AppDestination
import com.example.model.MediaCategory
import com.example.model.VaultMediaItem
import com.example.model.VideoMediaItem
import com.example.ui.components.BottomNavigationBar
import com.example.ui.components.BrandHeader
import com.example.ui.components.BulkDownloadButton
import com.example.ui.components.CustomVideoAppIcon
import com.example.ui.components.GradientActionButton
import com.example.ui.components.KuaishouParsedPanel
import com.example.ui.components.MediaCategorySelector
import com.example.ui.components.PadlockIcon
import com.example.ui.components.PlayCircleBadgeIcon
import com.example.ui.components.UrlInputField
import com.example.ui.components.VaultThumbnailCard
import com.example.ui.components.VideoThumbnailCard
import com.example.ui.theme.BrandActionGradient
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.ErrorText
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteText
import com.example.viewmodel.KuaiViewModel

@Composable
fun KuaiVideoSaveApp(
    viewModel: KuaiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(
        enabled = uiState.showBulkDownloadScreen || uiState.currentDestination != AppDestination.DOWNLOAD
    ) {
        if (uiState.showBulkDownloadScreen) {
            viewModel.closeBulkDownloadScreen()
        } else {
            viewModel.selectDestination(AppDestination.DOWNLOAD)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        containerColor = MainBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavigationBar(
                currentDestination = uiState.currentDestination,
                onDestinationSelected = viewModel::selectDestination
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MainBackground)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
            ) {
                when (uiState.currentDestination) {
                    AppDestination.DOWNLOAD -> {
                        if (uiState.showBulkDownloadScreen) {
                            BulkDownloadScreen(
                                urlInput = uiState.bulkUrlInput,
                                selectedContentType = uiState.bulkContentType,
                                selectedQuality = uiState.bulkSelectedQuality,
                                pendingItems = uiState.bulkPendingItems,
                                selectedCount = uiState.selectedBulkCount,
                                isDownloadingAll = uiState.isBulkDownloadingAll,
                                validationMessage = uiState.bulkValidationMessage,
                                isValidationError = uiState.isBulkValidationError,
                                onBackClick = viewModel::closeBulkDownloadScreen,
                                onUrlChange = viewModel::updateBulkUrlInput,
                                onAddUrlClick = viewModel::addBulkUrl,
                                onContentTypeSelect = viewModel::selectBulkContentType,
                                onQualitySelect = viewModel::selectBulkQuality,
                                onToggleItemSelection = viewModel::toggleBulkItemSelection,
                                onClearAllClick = viewModel::clearAllBulkItems,
                                onDownloadAllClick = viewModel::startBulkDownloadAll
                            )
                        } else {
                            DownloadScreen(
                                urlInput = uiState.urlInput,
                                bulkDownloadEnabled = uiState.bulkDownloadEnabled,
                                isDownloading = uiState.isDownloading,
                                validationMessage = uiState.validationMessage,
                                isValidationError = uiState.isValidationError,
                                parsedPost = uiState.parsedPost,
                                selectedQuality = uiState.selectedParsedQuality,
                                activeDownloadTask = uiState.activeDownloadTask,
                                onUrlChange = viewModel::updateUrlInput,
                                onPasteFromClipboard = viewModel::pasteUrlFromClipboard,
                                onDownloadClick = viewModel::startDownload,
                                onBulkDownloadClick = viewModel::onBulkDownloadClick,
                                onSelectQuality = viewModel::selectParsedVideoQuality,
                                onDownloadVideoNoWatermark = viewModel::downloadParsedVideoNoWatermark,
                                onDownloadOriginalAudio = viewModel::downloadParsedOriginalAudio,
                                onDownloadCoverThumbnail = viewModel::downloadParsedCoverThumbnail,
                                onDownloadAllPhotos = viewModel::downloadAllParsedPhotos,
                                onDownloadSinglePhoto = viewModel::downloadSingleParsedPhoto,
                                onPauseOrResumeDownload = viewModel::togglePauseResumeDownload,
                                onCancelDownload = viewModel::cancelActiveDownload
                            )
                        }
                    }

                    AppDestination.PLAYER -> {
                        PlayerScreen(
                            selectedCategory = uiState.playerCategory,
                            mediaItems = uiState.displayedPlayerItems,
                            onCategorySelected = viewModel::selectPlayerCategory,
                            onMediaItemClick = viewModel::openVideoPreview
                        )
                    }

                    AppDestination.VAULT -> {
                        VaultScreen(
                            selectedCategory = uiState.vaultCategory,
                            vaultItems = uiState.displayedVaultItems,
                            onCategorySelected = viewModel::selectVaultCategory,
                            onVaultItemClick = viewModel::openVaultPreview
                        )
                    }
                }
            }
        }
    }

    uiState.activeVideoPreview?.let { item ->
        VideoPreviewModal(
            item = item,
            onMoveToVault = { viewModel.moveToPrivateVault(item) },
            onDismiss = viewModel::closeVideoPreview
        )
    }

    uiState.activeVaultPreview?.let { item ->
        VaultPreviewModal(
            item = item,
            pinError = uiState.vaultPinError,
            onUnlockWithPin = viewModel::unlockVaultWithPin,
            onDismiss = viewModel::closeVaultPreview
        )
    }
}

@Composable
fun DownloadScreen(
    urlInput: String,
    bulkDownloadEnabled: Boolean,
    isDownloading: Boolean,
    validationMessage: String?,
    isValidationError: Boolean,
    onUrlChange: (String) -> Unit,
    onPasteFromClipboard: (String?) -> Unit,
    onDownloadClick: () -> Unit,
    onBulkDownloadClick: () -> Unit,
    parsedPost: KuaishouParsedPost? = null,
    selectedQuality: KuaishouVideoQualityStream? = null,
    activeDownloadTask: DownloadTaskState? = null,
    onSelectQuality: (KuaishouVideoQualityStream) -> Unit = {},
    onDownloadVideoNoWatermark: () -> Unit = {},
    onDownloadOriginalAudio: () -> Unit = {},
    onDownloadCoverThumbnail: () -> Unit = {},
    onDownloadAllPhotos: () -> Unit = {},
    onDownloadSinglePhoto: (KuaishouPhotoItem) -> Unit = {},
    onPauseOrResumeDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DesignTokens.SpaceMajor)
            .testTag("download_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(DesignTokens.HeaderTopSpacing))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("download_header_container"),
            contentAlignment = Alignment.Center
        ) {
            CustomVideoAppIcon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("custom_video_app_icon")
            )

            BrandHeader(
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.HeaderToBulkButtonGap))

        BulkDownloadButton(
            onClick = {
                focusManager.clearFocus()
                onBulkDownloadClick()
            }
        )

        Spacer(modifier = Modifier.height(DesignTokens.BulkButtonToUrlInputGap))

        UrlInputField(
            urlText = urlInput,
            onUrlChange = onUrlChange,
            onPasteClick = {
                val clipText = clipboardManager.getText()?.text
                onPasteFromClipboard(clipText)
            },
            onDoneAction = {
                focusManager.clearFocus()
                onDownloadClick()
            }
        )

        Spacer(modifier = Modifier.height(DesignTokens.InputToDownloadButtonGap))

        GradientActionButton(
            isLoading = isDownloading,
            onClick = {
                focusManager.clearFocus()
                onDownloadClick()
            }
        )

        if (!validationMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = validationMessage,
                style = MaterialTheme.typography.labelMedium,
                color = if (isValidationError) ErrorText else BrandGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("validation_message")
            )
        }

        if (parsedPost != null || activeDownloadTask != null) {
            Spacer(modifier = Modifier.height(14.dp))
            KuaishouParsedPanel(
                parsedPost = parsedPost,
                selectedQuality = selectedQuality,
                activeDownloadTask = activeDownloadTask,
                onSelectQuality = onSelectQuality,
                onDownloadVideoNoWatermark = onDownloadVideoNoWatermark,
                onDownloadOriginalAudio = onDownloadOriginalAudio,
                onDownloadCoverThumbnail = onDownloadCoverThumbnail,
                onDownloadAllPhotos = onDownloadAllPhotos,
                onDownloadSinglePhoto = onDownloadSinglePhoto,
                onPauseOrResumeDownload = onPauseOrResumeDownload,
                onCancelDownload = onCancelDownload
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PlayerScreen(
    selectedCategory: MediaCategory,
    mediaItems: List<VideoMediaItem>,
    onCategorySelected: (MediaCategory) -> Unit,
    onMediaItemClick: (VideoMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DesignTokens.GridHorizontalPadding)
            .testTag("player_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(DesignTokens.ScreenTopToCategoryGap))

        MediaCategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        Spacer(modifier = Modifier.height(DesignTokens.CategoryToGridGap))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_media_grid"),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGridGapVertical)
        ) {
            mediaItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGridGapHorizontal)
                ) {
                    rowItems.forEach { item ->
                        VideoThumbnailCard(
                            item = item,
                            onClick = { onMediaItemClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun VaultScreen(
    selectedCategory: MediaCategory,
    vaultItems: List<VaultMediaItem>,
    onCategorySelected: (MediaCategory) -> Unit,
    onVaultItemClick: (VaultMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DesignTokens.GridHorizontalPadding)
            .testTag("vault_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(DesignTokens.ScreenTopToCategoryGap))

        MediaCategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        Spacer(modifier = Modifier.height(DesignTokens.CategoryToGridGap))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_media_grid"),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGridGapVertical)
        ) {
            vaultItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGridGapHorizontal)
                ) {
                    rowItems.forEach { item ->
                        VaultThumbnailCard(
                            item = item,
                            onClick = { onVaultItemClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun VideoPreviewModal(
    item: VideoMediaItem,
    onMoveToVault: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MainBackground,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_preview_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = item.thumbnailRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    PlayCircleBadgeIcon(
                        isGradientSelected = true,
                        inactiveCircleColor = PrimaryOrange,
                        size = 54.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (item.isDemonstration) {
                        "Demonstration preview (${item.duration})"
                    } else {
                        buildString {
                            append("Saved Offline (${item.duration})")
                            item.qualityLabel?.let { append(" • $it") }
                            item.fileSizeText?.let { append(" • $it") }
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )

                if (!item.fileName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.fileName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("preview_saved_filename")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = BrandGreen)
                        .clickable(onClick = onMoveToVault)
                        .testTag("move_to_vault_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Move to Private Vault",
                        color = WhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush = BrandActionGradient)
                        .clickable(onClick = onDismiss)
                        .testTag("close_preview_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        color = WhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultPreviewModal(
    item: VaultMediaItem,
    pinError: String?,
    onUnlockWithPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MainBackground,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_preview_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = item.thumbnailRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (item.isLocked) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            PadlockIcon(
                                color = WhiteText,
                                keyholeColor = PrimaryOrange,
                                size = 28.dp
                            )
                        }
                    } else {
                        PlayCircleBadgeIcon(
                            isGradientSelected = true,
                            inactiveCircleColor = BrandGreen,
                            size = 54.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (item.isLocked) {
                        "PIN-Locked Private Vault Item"
                    } else {
                        buildString {
                            append("Unlocked Vault Media")
                            item.fileName?.let { append(" • $it") }
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isLocked) SecondaryText else BrandGreen,
                    textAlign = TextAlign.Center
                )

                if (item.isLocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BasicTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondarySurface)
                            .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("vault_pin_input"),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (pinInput.isEmpty()) {
                                    Text(
                                        text = "Enter 4-Digit Vault PIN (Default: 1234)",
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (!pinError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pinError,
                            fontSize = 11.sp,
                            color = ErrorText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandGreen)
                            .clickable { onUnlockWithPin(pinInput.ifEmpty { "1234" }) }
                            .testTag("unlock_vault_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unlock Vault Item",
                            color = WhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush = BrandActionGradient)
                        .clickable(onClick = onDismiss)
                        .testTag("close_vault_preview_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Close",
                        color = WhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
