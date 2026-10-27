package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ksdownloader.KSDownloaderBridge
import com.example.ksdownloader.KSLinkFormat
import com.example.ksdownloader.KSWorksType
import com.example.ui.screens.KuaiVideoSaveApp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w402dp-h874dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kuai video Save", appName)
    }

    @Test
    fun `verify KSDownloaderBridge 5 official link formats batch extraction and FastAPI 5557 contract`() = runBlocking {
        // 1. Verify all 5 official JoeanAmier/KS-Downloader link formats
        val link1 = "https://www.kuaishou.com/f/X-92aBcDeF"
        val link2 = "https://v.kuaishou.com/7xYz9A"
        val link3 = "https://www.kuaishou.com/short-video/3x89aBcDeF"
        val link4 = "https://kuaishou.cn/short-video/3x99CnPost"
        val link5 = "https://live.kuaishou.com/u/author_881/3xLivePost99"

        assertEquals(KSLinkFormat.SHARE_F, KSDownloaderBridge.classifyLinkFormat(link1))
        assertEquals(KSLinkFormat.SHORT_V, KSDownloaderBridge.classifyLinkFormat(link2))
        assertEquals(KSLinkFormat.WEB_SHORT_VIDEO, KSDownloaderBridge.classifyLinkFormat(link3))
        assertEquals(KSLinkFormat.CN_SHORT_VIDEO, KSDownloaderBridge.classifyLinkFormat(link4))
        assertEquals(KSLinkFormat.LIVE_USER_POST, KSDownloaderBridge.classifyLinkFormat(link5))

        // 2. Verify space-separated batch link extraction
        val batchInput = "$link1 $link2 $link3 $link4 $link5"
        val extractedBatch = KSDownloaderBridge.extractBatchUrls(batchInput)
        assertEquals(5, extractedBatch.size)

        // 3. Verify non-login visitor session cookies
        val cookies = KSDownloaderBridge.ensureNonLoginSessionCookies()
        assertTrue(cookies["did"]?.startsWith("web_") == true)
        assertTrue(cookies.containsKey("didv"))

        // 4. Verify video extraction & 4 media streams
        val messyShare = "🔥太震撼了！航拍绝美雪山湖泊 #快手创作者 @自然探索者 https://v.kuaishou.com/7xYz9A 复制此消息打开【快手】直接观看！"
        val videoWorks = KSDownloaderBridge.extractWorks(
            rawInput = messyShare,
            simulateNetworkDelayMs = 0L
        ).getOrThrow()

        assertEquals(KSWorksType.VIDEO, videoWorks.worksType)
        assertEquals("https://v.kuaishou.com/7xYz9A", videoWorks.worksUrl)
        assertEquals("自然探索者", videoWorks.authorNickname)
        assertTrue(videoWorks.videoQualities.size >= 4)
        assertTrue(videoWorks.musicUrl.endsWith(".mp3"))
        assertTrue(videoWorks.coverUrl.endsWith(".jpg"))

        val ksMap = videoWorks.toKSRepositoryMap()
        assertEquals(videoWorks.worksId, ksMap["作品ID"])
        assertEquals("视频", ksMap["作品类型"])

        // 5. Verify Photo Atlas (图集) extraction
        val photoShare = "📸绝美秋日图集分享 @风光摄影师 https://www.kuaishou.com/photo/atlas_99281"
        val photoWorks = KSDownloaderBridge.extractWorks(
            rawInput = photoShare,
            simulateNetworkDelayMs = 0L
        ).getOrThrow()
        assertEquals(KSWorksType.PHOTO_ATLAS, photoWorks.worksType)
        assertTrue(photoWorks.photoAtlas.isNotEmpty())

        // 6. Verify FastAPI Port 5557 Chinese JSON response parsing
        val fastApiJson = """
            {
              "message": "success",
              "data": {
                "作品ID": "3xFastApi992",
                "作品描述": "FastAPI 5557 Extracted Mountain Sunrise",
                "作品类型": "视频",
                "作者昵称": "JoeanStudio",
                "作者ID": "ks_joean_01",
                "下载地址": ["https://txmov2.a.kwimgs.com/upic/video_nowm.mp4"],
                "封面地址": "https://txmov2.a.kwimgs.com/upic/cover_hd.jpg",
                "音乐地址": "https://txmov2.a.kwimgs.com/upic/music_orig.mp3"
              }
            }
        """.trimIndent()
        val parsedFastApi = KSDownloaderBridge.tryParseKSExtractorPayload(
            payload = fastApiJson,
            cleanShareUrl = link3,
            resolvedPageUrl = link3,
            rawInput = link3,
            titleHint = null,
            authorHint = null
        )
        assertNotNull(parsedFastApi)
        assertEquals("3xFastApi992", parsedFastApi?.worksId)
        assertEquals("FastAPI 5557 Extracted Mountain Sunrise", parsedFastApi?.description)
        assertEquals("JoeanStudio", parsedFastApi?.authorNickname)
    }

    @Test
    fun `verify Download BulkDownload Player and Vault screens match specification`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                KuaiVideoSaveApp()
            }
        }

        composeTestRule.onNodeWithTag("custom_video_app_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("brand_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bulk Video Download").assertIsDisplayed()
        composeTestRule.onNodeWithTag("url_input_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("paste_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bulk_download_button").performClick()
        composeTestRule.onNodeWithTag("bulk_download_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_header_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_url_input_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_add_url_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bulk_type_video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_type_audio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_type_photo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_quality_360p").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_quality_480p").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_quality_720p").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_quality_1080p").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_quality_4k").assertIsDisplayed()

        composeTestRule.onNodeWithText("Video List (3)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beautiful Nature Video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunset Beach Video").assertIsDisplayed()
        composeTestRule.onNodeWithText("City Night View").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download All (3)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bulk_checkbox_bulk_1").performClick()
        composeTestRule.onNodeWithText("Download All (2)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bulk_checkbox_bulk_1").performClick()
        composeTestRule.onNodeWithText("Download All (3)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bulk_url_text_field")
            .performTextInput("🔥快手热门视频 https://v.kuaishou.com/ks8821")
        composeTestRule.onNodeWithTag("bulk_add_url_button").performClick()
        composeTestRule.onNodeWithText("Video List (4)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download All (4)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bulk_back_button").performClick()
        composeTestRule.onNodeWithTag("download_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_item_player").performClick()
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("brand_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("custom_video_app_icon").assertDoesNotExist()
        composeTestRule.onNodeWithTag("category_pill_videos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_pill_audio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_pill_images").assertIsDisplayed()
        composeTestRule.onNodeWithText("12:34").assertIsDisplayed()
        composeTestRule.onNodeWithText("08:45").assertIsDisplayed()
        composeTestRule.onNodeWithText("15:20").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_item_vault").performClick()
        composeTestRule.onNodeWithTag("vault_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("brand_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("custom_video_app_icon").assertDoesNotExist()
        composeTestRule.onNodeWithTag("category_pill_videos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vault_card_vault_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vault_card_vault_9").assertIsDisplayed()
    }
}
