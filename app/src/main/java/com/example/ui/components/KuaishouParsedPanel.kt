package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DownloadTaskState
import com.example.engine.DownloadTaskStatus
import com.example.engine.KuaishouParsedPost
import com.example.engine.KuaishouPhotoItem
import com.example.engine.KuaishouPostType
import com.example.engine.KuaishouVideoQualityStream
import com.example.ui.theme.BrandActionGradient
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.ErrorText
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.ThumbnailBadgeOverlay
import com.example.ui.theme.WhiteText

@Composable
fun KuaishouParsedPanel(
    parsedPost: KuaishouParsedPost?,
    selectedQuality: KuaishouVideoQualityStream?,
    activeDownloadTask: DownloadTaskState?,
    onSelectQuality: (KuaishouVideoQualityStream) -> Unit,
    onDownloadVideoNoWatermark: () -> Unit,
    onDownloadOriginalAudio: () -> Unit,
    onDownloadCoverThumbnail: () -> Unit,
    onDownloadAllPhotos: () -> Unit,
    onDownloadSinglePhoto: (KuaishouPhotoItem) -> Unit,
    onPauseOrResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeDownloadTask != null && activeDownloadTask.status != DownloadTaskStatus.IDLE) {
            RealTimeDownloadProgressCard(
                task = activeDownloadTask,
                onPauseOrResume = onPauseOrResumeDownload,
                onCancel = onCancelDownload
            )
        }

        if (parsedPost != null) {
            val cardShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MainBackground)
                    .border(width = 1.dp, color = SubtleBorder, shape = cardShape)
                    .padding(14.dp)
                    .testTag("parsed_preview_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDFF6E6))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("parsed_post_badge")
                        ) {
                            Text(
                                text = if (parsedPost.postType == KuaishouPostType.PHOTO_CAROUSEL) {
                                    "PHOTO CAROUSEL • NO WATERMARK"
                                } else {
                                    "VIDEO STREAM • NO WATERMARK"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                    }

                    Text(
                        text = "@${parsedPost.authorName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("parsed_post_author")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(92.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondarySurface)
                    ) {
                        Image(
                            painter = painterResource(id = parsedPost.thumbnailRes),
                            contentDescription = parsedPost.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ThumbnailBadgeOverlay)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = parsedPost.durationText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = parsedPost.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("parsed_post_title")
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = parsedPost.cleanShareUrl,
                            fontSize = 11.sp,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val activeSizeText = if (parsedPost.postType == KuaishouPostType.PHOTO_CAROUSEL) {
                            "${parsedPost.photoCarousel.size} HD Photos • Audio ${parsedPost.audioStream.formattedSize}"
                        } else {
                            val q = selectedQuality ?: parsedPost.availableQualities.firstOrNull()
                            "${q?.qualityLabel ?: "1080p Full HD"} • ${q?.formattedSize ?: "34.7 MB"}"
                        }
                        Text(
                            text = activeSizeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandGreen,
                            modifier = Modifier.testTag("parsed_selected_stream_meta")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (parsedPost.postType == KuaishouPostType.VIDEO) {
                    Text(
                        text = "Select Stream Quality (No Watermark)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parsed_quality_selector"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        parsedPost.availableQualities.forEach { stream ->
                            val isSelected = selectedQuality?.qualityId == stream.qualityId
                            val pillShape = RoundedCornerShape(9.dp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(pillShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(BrandGreen)
                                        } else {
                                            Modifier
                                                .background(SecondarySurface)
                                                .border(1.dp, SubtleBorder, pillShape)
                                        }
                                    )
                                    .clickable(
                                        role = Role.Tab,
                                        onClick = { onSelectQuality(stream) }
                                    )
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .testTag("parsed_quality_pill_${stream.shortBadge.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stream.shortBadge,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) WhiteText else PrimaryText
                                )
                                Text(
                                    text = stream.formattedSize,
                                    fontSize = 9.sp,
                                    color = if (isSelected) WhiteText.copy(alpha = 0.9f) else SecondaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeBadge = selectedQuality?.qualityLabel ?: "1080p Full HD"

                    ContextActionButton(
                        text = "Download Video ($activeBadge - No Watermark)",
                        backgroundBrush = BrandActionGradient,
                        textColor = WhiteText,
                        onClick = onDownloadVideoNoWatermark,
                        testTag = "btn_download_video_nowm"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContextActionSolidButton(
                        text = "Download Audio (Original MP3 • ${parsedPost.audioStream.formattedSize})",
                        backgroundColor = BrandGreen,
                        textColor = WhiteText,
                        onClick = onDownloadOriginalAudio,
                        testTag = "btn_download_audio_mp3"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContextActionOutlinedButton(
                        text = "Download Video Thumbnail (HD Cover • ${parsedPost.coverFormattedSize})",
                        borderColor = BrandBlue,
                        textColor = BrandBlue,
                        onClick = onDownloadCoverThumbnail,
                        testTag = "btn_download_cover_hd"
                    )
                } else {
                    Text(
                        text = "Photo Carousel Album (${parsedPost.photoCarousel.size} HD Images)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parsed_photo_carousel"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parsedPost.photoCarousel.take(4).forEach { photo ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecondarySurface)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
                                    .clickable { onDownloadSinglePhoto(photo) }
                                    .padding(4.dp)
                                    .testTag("btn_download_single_photo_${photo.index}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = photo.thumbnailRes),
                                    contentDescription = "Photo #${photo.index}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Save #${photo.index}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ContextActionButton(
                        text = "Download All Post Photos (HD • ${parsedPost.photoCarousel.size} Images)",
                        backgroundBrush = BrandActionGradient,
                        textColor = WhiteText,
                        onClick = onDownloadAllPhotos,
                        testTag = "btn_download_all_photos"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContextActionSolidButton(
                        text = "Download Audio (Original MP3 • ${parsedPost.audioStream.formattedSize})",
                        backgroundColor = BrandGreen,
                        textColor = WhiteText,
                        onClick = onDownloadOriginalAudio,
                        testTag = "btn_download_audio_mp3"
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTimeDownloadProgressCard(
    task: DownloadTaskState,
    onPauseOrResume: () -> Unit,
    onCancel: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SecondarySurface)
            .border(1.dp, SubtleBorder, shape)
            .padding(12.dp)
            .testTag("download_progress_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.fileName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("download_task_filename")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${task.progressPercent}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
                modifier = Modifier.testTag("download_task_percent")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = BrandGreen,
            trackColor = SubtleBorder
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when (task.status) {
                DownloadTaskStatus.DOWNLOADING -> "Streaming • ${task.formattedSpeed}"
                DownloadTaskStatus.PAUSED -> "Paused • Tap Resume"
                DownloadTaskStatus.COMPLETED -> "Saved to Offline Library • ${task.formattedSpeed}"
                DownloadTaskStatus.CANCELLED -> "Cancelled"
                DownloadTaskStatus.FAILED -> "Failed"
                DownloadTaskStatus.IDLE -> ""
            }
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.status == DownloadTaskStatus.CANCELLED || task.status == DownloadTaskStatus.FAILED) {
                    ErrorText
                } else {
                    SecondaryText
                },
                modifier = Modifier.testTag("download_task_speed")
            )

            if (task.status == DownloadTaskStatus.DOWNLOADING || task.status == DownloadTaskStatus.PAUSED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandBlue.copy(alpha = 0.12f))
                            .clickable(onClick = onPauseOrResume)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("btn_pause_resume_download")
                    ) {
                        Text(
                            text = if (task.status == DownloadTaskStatus.PAUSED) "Resume" else "Pause",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ErrorText.copy(alpha = 0.12f))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("btn_cancel_download")
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextActionButton(
    text: String,
    backgroundBrush: androidx.compose.ui.graphics.Brush,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(brush = backgroundBrush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContextActionSolidButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(color = backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContextActionOutlinedButton(
    text: String,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(color = SecondarySurface)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
