package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BulkContentType
import com.example.model.BulkItemStatus
import com.example.model.BulkPendingItem
import com.example.ui.components.CategoryAudioMusicIcon
import com.example.ui.components.CategoryImagesGalleryIcon
import com.example.ui.components.DownloadTrayIcon
import com.example.ui.components.LinkChainIcon
import com.example.ui.theme.BrandActionGradient
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandUnderlineGradient
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.ErrorText
import com.example.ui.theme.InactiveCategoryIcon
import com.example.ui.theme.InputBackground
import com.example.ui.theme.MainBackground
import com.example.ui.theme.OuterInputHalo
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.ThumbnailBadgeOverlay
import com.example.ui.theme.WhiteText

@Composable
fun BulkDownloadScreen(
    urlInput: String,
    selectedContentType: BulkContentType,
    selectedQuality: String,
    pendingItems: List<BulkPendingItem>,
    selectedCount: Int,
    isDownloadingAll: Boolean,
    validationMessage: String?,
    isValidationError: Boolean,
    onBackClick: () -> Unit,
    onUrlChange: (String) -> Unit,
    onAddUrlClick: () -> Unit,
    onContentTypeSelect: (BulkContentType) -> Unit,
    onQualitySelect: (String) -> Unit,
    onToggleItemSelection: (String) -> Unit,
    onClearAllClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(horizontal = 16.dp)
            .testTag("bulk_download_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BulkDownloadHeader(onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(12.dp))

            BulkUrlInputRow(
                urlText = urlInput,
                onUrlChange = onUrlChange,
                onAddClick = {
                    focusManager.clearFocus()
                    onAddUrlClick()
                }
            )

            if (!validationMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = validationMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isValidationError) ErrorText else BrandGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_validation_message")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Content Type",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_content_type_label")
            )

            Spacer(modifier = Modifier.height(8.dp))

            BulkContentTypeSelector(
                selectedContentType = selectedContentType,
                onContentTypeSelected = onContentTypeSelect
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Quality",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_quality_label")
            )

            Spacer(modifier = Modifier.height(8.dp))

            BulkQualitySelector(
                qualities = selectedContentType.qualities,
                selectedQuality = selectedQuality,
                onQualitySelected = onQualitySelect
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedContentType.label} List (${pendingItems.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier.testTag("bulk_list_title")
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            onClick = onClearAllClick
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("bulk_clear_all_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrashOutlineIcon(
                        color = SecondaryText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Clear All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pendingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondarySurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .testTag("bulk_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending items in the list. Paste a URL above and tap + to add.",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_pending_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingItems.forEach { item ->
                        BulkPendingItemCard(
                            item = item,
                            onToggleSelect = { onToggleItemSelection(item.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        BulkDownloadAllButton(
            selectedCount = selectedCount,
            isLoading = isDownloadingAll,
            onClick = onDownloadAllClick
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BulkDownloadHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bulk_header"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = onBackClick
                )
                .testTag("bulk_back_button"),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.66f, h * 0.16f)
                    lineTo(w * 0.28f, h * 0.50f)
                    lineTo(w * 0.66f, h * 0.84f)
                }
                drawPath(
                    path = path,
                    color = PrimaryText,
                    style = Stroke(
                        width = w * 0.12f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = PrimaryOrange, fontWeight = FontWeight.Bold)) {
                        append("Bulk")
                    }
                    append(" ")
                    withStyle(SpanStyle(color = BrandBlue, fontWeight = FontWeight.Bold)) {
                        append("Video")
                    }
                    append(" ")
                    withStyle(SpanStyle(color = BrandGreen, fontWeight = FontWeight.Bold)) {
                        append("Download")
                    }
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.testTag("bulk_header_title")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(DesignTokens.HeaderUnderlineWidth)
                    .height(DesignTokens.HeaderUnderlineHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(brush = BrandUnderlineGradient)
            )
        }
    }
}

@Composable
private fun BulkUrlInputRow(
    urlText: String,
    onUrlChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(16.dp)
    val innerShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = OuterInputHalo, shape = outerShape)
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(innerShape)
                .background(InputBackground)
                .border(width = 1.dp, color = SubtleBorder, shape = innerShape)
                .padding(start = 14.dp, end = 8.dp)
                .testTag("bulk_url_input_container"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinkChainIcon(
                color = SecondaryText,
                size = 20.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (urlText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.placeholder_paste_url),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = urlText,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = PrimaryText,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PrimaryOrange),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAddClick() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_url_text_field")
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(brush = BrandActionGradient)
                    .clickable(
                        role = Role.Button,
                        onClick = onAddClick
                    )
                    .testTag("bulk_add_url_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(17.dp)) {
                    val w = size.width
                    val h = size.height
                    val stroke = w * 0.14f
                    drawLine(
                        color = WhiteText,
                        start = Offset(w * 0.50f, h * 0.16f),
                        end = Offset(w * 0.50f, h * 0.84f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = WhiteText,
                        start = Offset(w * 0.16f, h * 0.50f),
                        end = Offset(w * 0.84f, h * 0.50f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkContentTypeSelector(
    selectedContentType: BulkContentType,
    onContentTypeSelected: (BulkContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bulk_content_type_selector"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BulkContentType.entries.forEach { type ->
            val isSelected = type == selectedContentType
            val pillShape = RoundedCornerShape(12.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(pillShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(brush = BrandActionGradient)
                        } else {
                            Modifier
                                .background(color = SecondarySurface)
                                .border(width = 1.dp, color = SubtleBorder, shape = pillShape)
                        }
                    )
                    .clickable(
                        role = Role.Tab,
                        onClick = { onContentTypeSelected(type) }
                    )
                    .padding(horizontal = 8.dp)
                    .testTag("bulk_type_${type.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (type) {
                    BulkContentType.VIDEO -> BulkVideoSquarePlayIcon(
                        selected = isSelected,
                        inactiveColor = InactiveCategoryIcon
                    )
                    BulkContentType.AUDIO -> CategoryAudioMusicIcon(
                        color = if (isSelected) WhiteText else InactiveCategoryIcon,
                        size = 18.dp
                    )
                    BulkContentType.PHOTO -> CategoryImagesGalleryIcon(
                        color = if (isSelected) WhiteText else InactiveCategoryIcon,
                        size = 18.dp
                    )
                }

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = type.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) WhiteText else PrimaryText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BulkVideoSquarePlayIcon(
    selected: Boolean,
    inactiveColor: Color
) {
    val bgColor = if (selected) WhiteText else inactiveColor
    val triangleColor = if (selected) PrimaryOrange else WhiteText
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.14f)
                lineTo(w * 0.88f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.86f)
                close()
            }
            drawPath(path = path, color = triangleColor)
        }
    }
}

@Composable
private fun BulkQualitySelector(
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bulk_quality_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        qualities.forEach { quality ->
            val isSelected = quality == selectedQuality
            val shape = RoundedCornerShape(10.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(shape)
                    .then(
                        if (isSelected) {
                            Modifier.background(color = BrandGreen)
                        } else {
                            Modifier
                                .background(color = SecondarySurface)
                                .border(width = 1.dp, color = SubtleBorder, shape = shape)
                        }
                    )
                    .clickable(
                        role = Role.Tab,
                        onClick = { onQualitySelected(quality) }
                    )
                    .testTag("bulk_quality_${quality.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quality,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) WhiteText else PrimaryText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BulkPendingItemCard(
    item: BulkPendingItem,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MainBackground)
            .border(width = 1.dp, color = SubtleBorder, shape = cardShape)
            .clickable(onClick = onToggleSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("bulk_item_card_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .then(
                    if (item.isSelected) {
                        Modifier.background(BrandGreen)
                    } else {
                        Modifier
                            .background(MainBackground)
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(5.dp))
                    }
                )
                .clickable(
                    role = Role.Checkbox,
                    onClick = onToggleSelect
                )
                .testTag("bulk_checkbox_${item.id}"),
            contentAlignment = Alignment.Center
        ) {
            if (item.isSelected) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val w = size.width
                    val h = size.height
                    val checkPath = Path().apply {
                        moveTo(w * 0.16f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.78f)
                        lineTo(w * 0.86f, h * 0.24f)
                    }
                    drawPath(
                        path = checkPath,
                        color = WhiteText,
                        style = Stroke(
                            width = w * 0.18f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(88.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SecondarySurface)
        ) {
            Image(
                painter = painterResource(id = item.thumbnailRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(ThumbnailBadgeOverlay)
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = item.duration,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = item.url,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileDocumentIcon(
                    color = SecondaryText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.fileSize,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDFF6E6))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.quality,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }

                if (item.status == BulkItemStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.progressPercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                } else if (item.status == BulkItemStatus.COMPLETED) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val chevron = Path().apply {
                moveTo(w * 0.32f, h * 0.18f)
                lineTo(w * 0.72f, h * 0.50f)
                lineTo(w * 0.32f, h * 0.82f)
            }
            drawPath(
                path = chevron,
                color = SecondaryText,
                style = Stroke(
                    width = w * 0.13f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
private fun BulkDownloadAllButton(
    selectedCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = PrimaryOrange.copy(alpha = 0.22f),
                spotColor = BrandGreen.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(brush = BrandActionGradient)
            .clickable(
                enabled = !isLoading,
                role = Role.Button,
                onClick = onClick
            )
            .testTag("bulk_download_all_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = WhiteText,
                    strokeWidth = 2.2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Downloading ($selectedCount)...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
            } else {
                DownloadTrayIcon(
                    color = WhiteText,
                    size = 21.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Download All ($selectedCount)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
            }
        }
    }
}

@Composable
private fun TrashOutlineIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.24f),
            end = Offset(w * 0.84f, h * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.10f),
            size = Size(w * 0.28f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.24f, h * 0.24f),
            size = Size(w * 0.52f, h * 0.66f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.40f),
            end = Offset(w * 0.42f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.40f),
            end = Offset(w * 0.58f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun FileDocumentIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.10f),
            size = Size(w * 0.64f, h * 0.80f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.38f),
            end = Offset(w * 0.66f, h * 0.38f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.58f),
            end = Offset(w * 0.66f, h * 0.58f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
