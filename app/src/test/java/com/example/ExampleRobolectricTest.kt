package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiResponse
import com.example.ai.GeminiClient
import com.example.data.crypto.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jarvis", appName)
    }

    @Test
    fun testCryptoManagerEncryptDecrypt() {
        val originalText = "Top secret Stark Industries blueprints 2026"
        val encrypted = CryptoManager.encrypt(originalText)
        val decrypted = CryptoManager.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testOfflineRuleEngineIdentity() {
        val geminiClient = GeminiClient()
        val response = geminiClient.processOfflineRuleEngine("who are you")
        assertTrue(response is AiResponse.Success)
        val success = response as AiResponse.Success
        assertTrue(success.replyText.contains("JARVIS", ignoreCase = true))
    }

    @Test
    fun testOfflineRuleEngineOpenApp() {
        val geminiClient = GeminiClient()
        val response = geminiClient.processOfflineRuleEngine("open Camera")
        assertTrue(response is AiResponse.Success)
        val success = response as AiResponse.Success
        assertNotNull(success.toolCall)
        assertEquals("open_app", success.toolCall?.name)
        assertEquals("camera", success.toolCall?.arguments?.get("app_name")?.lowercase())
    }

    @Test
    fun testVoiceInputToGeminiPipeline() {
        val geminiClient = GeminiClient()
        // Simulate speech recognition output passing to Gemini client
        val recognizedVoiceInput = "What time is it"
        val response = geminiClient.processOfflineRuleEngine(recognizedVoiceInput)
        assertTrue(response is AiResponse.Success)
        val success = response as AiResponse.Success
        assertTrue(success.replyText.contains("time is currently", ignoreCase = true))
    }
}
