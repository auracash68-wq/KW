package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.engine.KuaishouEngineService
import com.example.engine.KuaishouPostType
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
    fun `verify KuaishouEngineService share text cleaner session cookies and multi-layer parser`() = runBlocking {
        val messyShare = "🔥太震撼了！航拍绝美雪山湖泊 #快手创作者 @自然探索者 https://v.kuaishou.com/7xYz9A 复制此消息打开【快手】直接观看！"
        val cleanedUrl = KuaishouEngineService.cleanShareText(messyShare)
        assertEquals("https://v.kuaishou.com/7xYz9A", cleanedUrl)

        val (titleHint, authorHint) = KuaishouEngineService.extractShareTextMetadata(messyShare, cleanedUrl!!)
        assertNotNull(titleHint)
        assertEquals("自然探索者", authorHint)

        val cookies = KuaishouEngineService.ensureVisitorSessionCookies()
        assertTrue(cookies["did"]?.startsWith("web_") == true)
        assertTrue(cookies.containsKey("didv"))

        val videoParsed = KuaishouEngineService.parseUrl(
            rawInput = messyShare,
            simulateNetworkDelayMs = 0L
        ).getOrThrow()
        assertEquals(KuaishouPostType.VIDEO, videoParsed.postType)
        assertEquals("https://v.kuaishou.com/7xYz9A", videoParsed.cleanShareUrl)
        assertTrue(videoParsed.availableQualities.size >= 4)
        assertTrue(videoParsed.audioStream.audioUrl.endsWith(".mp3"))
        assertTrue(videoParsed.coverUrl.endsWith(".jpg"))

        val cleanMp4Name = videoParsed.buildCleanFileName("mp4", "1080p")
        assertTrue(cleanMp4Name.startsWith("KuaiSave_"))
        assertTrue(cleanMp4Name.endsWith("_1080p.mp4"))

        val photoShare = "📸绝美秋日图集分享 @风光摄影师 https://www.kuaishou.com/photo/atlas_99281"
        val photoParsed = KuaishouEngineService.parseUrl(
            rawInput = photoShare,
            simulateNetworkDelayMs = 0L
        ).getOrThrow()
        assertEquals(KuaishouPostType.PHOTO_CAROUSEL, photoParsed.postType)
        assertTrue(photoParsed.photoCarousel.isNotEmpty())

        val mockHtml = """
            <html><head><script>
            window.__INITIAL_STATE__ = {"visionVideoDetail":{"photo":{"id":"3x89aBc","caption":"Mountain Sunrise 4K","photoUrl":"https://tx2.a.kwimgs.com/upic/video_nowm.mp4","coverUrls":[{"url":"https://tx2.a.kwimgs.com/upic/cover.jpg"}],"soundTrack":{"audioUrls":[{"url":"https://tx2.a.kwimgs.com/upic/audio.mp3"}]}},"author":{"name":"AlpineStudio"}}};
            </script></head></html>
        """.trimIndent()
        val stateParsed = KuaishouEngineService.tryParseHtmlOrJsonPayload(
            payload = mockHtml,
            cleanShareUrl = "https://v.kuaishou.com/3x89aBc",
            resolvedPageUrl = "https://www.kuaishou.com/short-video/3x89aBc",
            rawInput = "https://v.kuaishou.com/3x89aBc",
            titleHint = null,
            authorHint = null,
            sourceTag = "UnitTest"
        )
        assertNotNull(stateParsed)
        assertEquals("Mountain Sunrise 4K", stateParsed?.title)
        assertEquals("AlpineStudio", stateParsed?.authorName)
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
