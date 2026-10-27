package com.example.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppDestination
import com.example.model.MediaCategory
import com.example.model.VaultMediaItem
import com.example.model.VideoMediaItem
import com.example.ui.theme.BrandActionGradient
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandUnderlineGradient
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.InactiveBottomNavIcon
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
fun CustomVideoAppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(13.dp),
                ambientColor = Color(0x33FF3B0B),
                spotColor = Color(0x44FF3B0B)
            )
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFFF3B0B)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height

            val bodyLeft = w * 0.08f
            val bodyTop = h * 0.22f
            val bodyWidth = w * 0.58f
            val bodyHeight = h * 0.56f
            val corner = w * 0.14f

            drawRoundRect(
                color = WhiteText,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(corner, corner)
            )

            val lensPath = Path().apply {
                moveTo(w * 0.68f, h * 0.38f)
                lineTo(w * 0.94f, h * 0.22f)
                lineTo(w * 0.94f, h * 0.78f)
                lineTo(w * 0.68f, h * 0.62f)
                close()
            }
            drawPath(path = lensPath, color = WhiteText)
        }
    }
}

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag("brand_header"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = PrimaryOrange, fontWeight = FontWeight.Bold)) {
                    append("Kuai")
                }
                append(" ")
                withStyle(SpanStyle(color = BrandBlue, fontWeight = FontWeight.Bold)) {
                    append("video")
                }
                append(" ")
                withStyle(SpanStyle(color = BrandGreen, fontWeight = FontWeight.Bold)) {
                    append("Save")
                }
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(DesignTokens.HeaderUnderlineWidth)
                .height(DesignTokens.HeaderUnderlineHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(brush = BrandUnderlineGradient)
        )
    }
}

@Composable
fun BulkDownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 2.dp,
                shape = buttonShape,
                ambientColor = BrandGreen.copy(alpha = 0.2f),
                spotColor = BrandGreen.copy(alpha = 0.3f)
            )
            .clip(buttonShape)
            .background(color = BrandGreen)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp)
            .testTag("bulk_download_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bulk Video Download",
            color = WhiteText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UrlInputField(
    urlText: String,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDoneAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = OuterInputHalo, shape = outerShape)
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(innerShape)
                .background(InputBackground)
                .border(width = 1.dp, color = SubtleBorder, shape = innerShape)
                .padding(start = 14.dp, end = 8.dp)
                .testTag("url_input_container"),
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
                    keyboardActions = KeyboardActions(onDone = { onDoneAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_text_field")
                )
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(brush = BrandActionGradient)
                    .clickable(
                        role = Role.Button,
                        onClick = onPasteClick
                    )
                    .padding(horizontal = 14.dp)
                    .testTag("paste_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Paste",
                    color = WhiteText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun GradientActionButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 6.dp,
                shape = buttonShape,
                ambientColor = PrimaryOrange.copy(alpha = 0.22f),
                spotColor = BrandGreen.copy(alpha = 0.22f)
            )
            .clip(buttonShape)
            .background(brush = BrandActionGradient)
            .clickable(
                enabled = !isLoading,
                role = Role.Button,
                onClick = onClick
            )
            .testTag("download_button"),
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
                    text = "Processing...",
                    color = WhiteText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                DownloadTrayIcon(
                    color = WhiteText,
                    size = 22.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Download",
                    color = WhiteText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MediaCategorySelector(
    selectedCategory: MediaCategory,
    onCategorySelected: (MediaCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_category_selector"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            val pillShape = RoundedCornerShape(12.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
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
                        onClick = { onCategorySelected(category) }
                    )
                    .padding(horizontal = 8.dp)
                    .testTag("category_pill_${category.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (category) {
                    MediaCategory.VIDEOS -> CategoryVideosPlayIcon(
                        selected = isSelected,
                        inactiveColor = InactiveCategoryIcon
                    )
                    MediaCategory.AUDIO -> CategoryAudioMusicIcon(
                        color = if (isSelected) WhiteText else InactiveCategoryIcon,
                        size = 18.dp
                    )
                    MediaCategory.IMAGES -> CategoryImagesGalleryIcon(
                        color = if (isSelected) WhiteText else InactiveCategoryIcon,
                        size = 18.dp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = category.label,
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
fun VideoThumbnailCard(
    item: VideoMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(SecondarySurface)
            .clickable(onClick = onClick)
            .testTag("player_card_${item.id}")
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
            size = 38.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ThumbnailBadgeOverlay)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.duration,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WhiteText,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VaultThumbnailCard(
    item: VaultMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(SecondarySurface)
            .clickable(onClick = onClick)
            .testTag("vault_card_${item.id}")
    ) {
        Image(
            painter = painterResource(id = item.thumbnailRes),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryOrange),
            contentAlignment = Alignment.Center
        ) {
            PadlockIcon(
                color = WhiteText,
                keyholeColor = PrimaryOrange,
                size = 20.dp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MainBackground)
            .border(width = 1.dp, color = SubtleBorder)
            .padding(horizontal = 8.dp)
            .testTag("bottom_navigation_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Download",
            isSelected = currentDestination == AppDestination.DOWNLOAD,
            onClick = { onDestinationSelected(AppDestination.DOWNLOAD) },
            modifier = Modifier.weight(1f),
            testTag = "nav_item_download"
        ) { isSelected ->
            NavDownloadIcon(
                color = if (isSelected) PrimaryOrange else InactiveBottomNavIcon,
                size = 24.dp
            )
        }

        BottomNavItem(
            label = "Player",
            isSelected = currentDestination == AppDestination.PLAYER,
            onClick = { onDestinationSelected(AppDestination.PLAYER) },
            modifier = Modifier.weight(1f),
            testTag = "nav_item_player"
        ) { isSelected ->
            NavPlayerIcon(
                color = if (isSelected) PrimaryOrange else InactiveBottomNavIcon,
                size = 24.dp
            )
        }

        BottomNavItem(
            label = "Vault",
            isSelected = currentDestination == AppDestination.VAULT,
            onClick = { onDestinationSelected(AppDestination.VAULT) },
            modifier = Modifier.weight(1f),
            testTag = "nav_item_vault"
        ) { isSelected ->
            NavVaultIcon(
                color = if (isSelected) PrimaryOrange else InactiveBottomNavIcon,
                size = 24.dp
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    icon: @Composable (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(isSelected)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryOrange else InactiveBottomNavIcon
        )
    }
}

// Vector Icon implementations
@Composable
fun LinkChainIcon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        val path1 = Path().apply {
            moveTo(w * 0.60f, h * 0.40f)
            lineTo(w * 0.70f, h * 0.30f)
            cubicTo(w * 0.85f, h * 0.15f, w * 0.95f, h * 0.25f, w * 0.80f, h * 0.40f)
            lineTo(w * 0.65f, h * 0.55f)
        }
        drawPath(path = path1, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val path2 = Path().apply {
            moveTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.30f, h * 0.70f)
            cubicTo(w * 0.15f, h * 0.85f, w * 0.05f, h * 0.75f, w * 0.20f, h * 0.60f)
            lineTo(w * 0.35f, h * 0.45f)
        }
        drawPath(path = path2, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.62f),
            end = Offset(w * 0.62f, h * 0.38f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun DownloadTrayIcon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.14f),
            end = Offset(w * 0.50f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.30f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.70f, h * 0.44f)
        }
        drawPath(path = arrowHead, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val trayPath = Path().apply {
            moveTo(w * 0.18f, h * 0.64f)
            lineTo(w * 0.18f, h * 0.84f)
            lineTo(w * 0.82f, h * 0.84f)
            lineTo(w * 0.82f, h * 0.64f)
        }
        drawPath(path = trayPath, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PlayCircleBadgeIcon(
    isGradientSelected: Boolean,
    inactiveCircleColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x44000000)
            )
            .clip(CircleShape)
            .then(
                if (isGradientSelected) {
                    Modifier.background(brush = BrandActionGradient)
                } else {
                    Modifier.background(color = inactiveCircleColor)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.42f)) {
            val w = this.size.width
            val h = this.size.height
            val triangle = Path().apply {
                moveTo(w * 0.22f, h * 0.12f)
                lineTo(w * 0.90f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.88f)
                close()
            }
            drawPath(path = triangle, color = WhiteText)
        }
    }
}

@Composable
fun PadlockIcon(color: Color, keyholeColor: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.26f, h * 0.10f),
            size = Size(w * 0.48f, h * 0.44f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.16f, h * 0.36f),
            size = Size(w * 0.68f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )

        drawCircle(
            color = keyholeColor,
            radius = w * 0.08f,
            center = Offset(w * 0.50f, h * 0.58f)
        )

        drawLine(
            color = keyholeColor,
            start = Offset(w * 0.50f, h * 0.58f),
            end = Offset(w * 0.50f, h * 0.72f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CategoryVideosPlayIcon(selected: Boolean, inactiveColor: Color) {
    val bgColor = if (selected) WhiteText else inactiveColor
    val triangleColor = if (selected) PrimaryOrange else WhiteText
    Box(
        modifier = Modifier
            .size(19.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            val w = this.size.width
            val h = this.size.height
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
fun CategoryAudioMusicIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f

        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.30f, h * 0.75f)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.44f, h * 0.75f),
            end = Offset(w * 0.44f, h * 0.20f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val flagPath = Path().apply {
            moveTo(w * 0.44f, h * 0.20f)
            cubicTo(w * 0.65f, h * 0.15f, w * 0.85f, h * 0.25f, w * 0.82f, h * 0.45f)
        }
        drawPath(path = flagPath, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun CategoryImagesGalleryIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.15f),
            size = Size(w * 0.80f, h * 0.70f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = stroke)
        )

        drawCircle(
            color = color,
            radius = w * 0.10f,
            center = Offset(w * 0.34f, h * 0.40f)
        )

        val mtn = Path().apply {
            moveTo(w * 0.18f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.58f, h * 0.64f)
            lineTo(w * 0.72f, h * 0.54f)
            lineTo(w * 0.82f, h * 0.74f)
        }
        drawPath(path = mtn, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun NavDownloadIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f

        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.18f),
            end = Offset(w * 0.50f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val arrow = Path().apply {
            moveTo(w * 0.32f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.46f)
        }
        drawPath(path = arrow, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val tray = Path().apply {
            moveTo(w * 0.20f, h * 0.66f)
            lineTo(w * 0.20f, h * 0.82f)
            lineTo(w * 0.80f, h * 0.82f)
            lineTo(w * 0.80f, h * 0.66f)
        }
        drawPath(path = tray, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun NavPlayerIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f

        drawCircle(
            color = color,
            radius = w * 0.38f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = stroke)
        )

        val tri = Path().apply {
            moveTo(w * 0.42f, h * 0.34f)
            lineTo(w * 0.66f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.66f)
            close()
        }
        drawPath(path = tri, color = color)
    }
}

@Composable
fun NavVaultIcon(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.16f),
            size = Size(w * 0.40f, h * 0.38f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.38f),
            size = Size(w * 0.56f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
            style = Stroke(width = stroke)
        )

        drawCircle(
            color = color,
            radius = w * 0.07f,
            center = Offset(w * 0.50f, h * 0.58f)
        )
    }
}
