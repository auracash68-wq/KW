package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Reference tokens matching the exact UI design
val PrimaryOrange = Color(0xFFFF6A00)
val BrandBlue = Color(0xFF0B63E5)
val BrandGreen = Color(0xFF168D3B)

val MainBackground = Color(0xFFFFFFFF)
val SecondarySurface = Color(0xFFF7F9FB)
val InputBackground = Color(0xFFF4F6F8)
val SubtleBorder = Color(0xFFE2E8F0)
val OuterInputHalo = Color(0xFFF1F5F9)

val PrimaryText = Color(0xFF0F172A)
val SecondaryText = Color(0xFF64748B)
val WhiteText = Color(0xFFFFFFFF)
val ErrorText = Color(0xFFDC2626)

val InactiveCategoryIcon = Color(0xFF475569)
val InactiveBottomNavIcon = Color(0xFF94A3B8)
val ThumbnailBadgeOverlay = Color(0xCC000000)

val BrandActionGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryOrange, BrandGreen)
)

val BrandUnderlineGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.0f to PrimaryOrange,
        0.34f to PrimaryOrange,
        0.341f to Color(0xFFCBD5E1),
        0.66f to Color(0xFFCBD5E1),
        0.661f to BrandGreen,
        1.0f to BrandGreen
    )
)

object DesignTokens {
    val HeaderTopSpacing = 16.dp
    val HeaderUnderlineWidth = 142.dp
    val HeaderUnderlineHeight = 3.5.dp

    val SpaceMajor = 20.dp
    val HeaderToBulkButtonGap = 16.dp
    val BulkButtonToUrlInputGap = 16.dp
    val InputToDownloadButtonGap = 16.dp

    val ScreenTopToCategoryGap = 16.dp
    val CategoryToGridGap = 16.dp

    val GridHorizontalPadding = 12.dp
    val SpaceGridGapHorizontal = 8.dp
    val SpaceGridGapVertical = 10.dp
}
