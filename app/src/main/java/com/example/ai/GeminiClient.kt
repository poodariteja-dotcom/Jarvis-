package com.example.ai

import com.example.BuildConfig
import com.example.data.repository.ChatMessage
import com.example.data.repository.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class AiResponse {
    data class Success(
        val replyText: String,
        val toolCall: ToolCall? = null
    ) : AiResponse()

    data class Error(val message: String) : AiResponse()
}

data class ToolCall(
    val name: String,
    val arguments: Map<String, String>
)

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        customApiKey: String = ""
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            isBuildConfigApiKeyValid() -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }

        // If no API key or offline, use Jarvis Offline Intelligence Rule Engine
        if (apiKey.isBlank()) {
            return@withContext processOfflineRuleEngine(prompt, history)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val requestBodyJson = buildRequestBody(prompt, history, systemInstruction)
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                // If API returns an error (quota, key invalid, etc.), fallback to offline NLP engine smoothly
                val offlineFallback = processOfflineRuleEngine(prompt, history)
                return@withContext if (offlineFallback is AiResponse.Success) {
                    offlineFallback.copy(replyText = "${offlineFallback.replyText} (Local fallback)")
                } else {
                    AiResponse.Error("Network error: ${response.code} ${response.message}")
                }
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            // Offline fallback on connection failure
            val offlineFallback = processOfflineRuleEngine(prompt, history)
            if (offlineFallback is AiResponse.Success) {
                offlineFallback
            } else {
                AiResponse.Error(e.message ?: "Unknown communication failure")
            }
        }
    }

    private fun isBuildConfigApiKeyValid(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.contains("TODO")
        } catch (_: Exception) {
            false
        }
    }

    private fun buildRequestBody(
        currentPrompt: String,
        history: List<ChatMessage>,
        systemInstructionText: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemInstructionText))
        systemInstruction.put("parts", sysParts)
        root.put("systemInstruction", systemInstruction)

        // Contents (Multi-turn conversation history with strict alternating user/model roles)
        val contents = JSONArray()
        val sanitizedTurns = mutableListOf<Pair<String, String>>()

        // Take recent history excluding the final prompt if already inserted into DB
        val recentItems = history.takeLast(12)
        for (item in recentItems) {
            val role = if (item.role == MessageRole.USER) "user" else "model"
            val text = item.content.trim()
            if (text.isBlank()) continue
            // Skip trailing user turn if identical to current prompt
            if (role == "user" && text == currentPrompt.trim()) continue

            if (sanitizedTurns.isEmpty()) {
                if (role == "user") {
                    sanitizedTurns.add(role to text)
                }
            } else {
                val lastRole = sanitizedTurns.last().first
                if (lastRole != role) {
                    sanitizedTurns.add(role to text)
                }
            }
        }

        // If the last history turn is 'user', pop it so currentTurn (which is 'user') alternates correctly
        if (sanitizedTurns.isNotEmpty() && sanitizedTurns.last().first == "user") {
            sanitizedTurns.removeAt(sanitizedTurns.lastIndex)
        }

        // Add history turns
        for ((role, text) in sanitizedTurns) {
            val turn = JSONObject()
            turn.put("role", role)
            val parts = JSONArray()
            parts.put(JSONObject().put("text", text))
            turn.put("parts", parts)
            contents.put(turn)
        }

        // Add current prompt as final user turn
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val curParts = JSONArray()
        curParts.put(JSONObject().put("text", currentPrompt))
        currentTurn.put("parts", curParts)
        contents.put(currentTurn)

        root.put("contents", contents)

        // Function Tools Declaration
        val tools = JSONArray()
        val functionDeclarations = JSONArray()

        functionDeclarations.put(
            buildToolDef("open_app", "Open or launch an application on the device", "app_name", "Name of the app, e.g. Camera, YouTube, Maps, Spotify")
        )
        functionDeclarations.put(
            buildToolDef("make_call", "Initiate a phone call to a contact or phone number", "phone_number", "Phone number or contact name")
        )
        functionDeclarations.put(
            buildSmsToolDef()
        )
        functionDeclarations.put(
            buildEmailToolDef()
        )
        functionDeclarations.put(
            buildToolDef("set_alarm", "Set an alarm for a specific time", "time", "Alarm time, e.g. 07:30 or 7:00 AM")
        )
        functionDeclarations.put(
            buildToolDef("device_action", "Execute a system or navigation action", "action", "Action name: go_home, go_back, show_recents, open_notifications, open_quick_settings, lock_screen")
        )
        functionDeclarations.put(
            buildToolDef("save_note", "Save a personal quick note to encrypted storage", "content", "Note contents")
        )
        functionDeclarations.put(
            buildMemoryToolDef()
        )

        val toolObj = JSONObject()
        toolObj.put("functionDeclarations", functionDeclarations)
        tools.put(toolObj)
        root.put("tools", tools)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("topP", 0.95)
        root.put("generationConfig", genConfig)

        return root
    }

    private fun buildToolDef(name: String, desc: String, paramName: String, paramDesc: String): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("description", desc)
        val params = JSONObject()
        params.put("type", "OBJECT")
        val props = JSONObject()
        val propObj = JSONObject()
        propObj.put("type", "STRING")
        propObj.put("description", paramDesc)
        props.put(paramName, propObj)
        params.put("properties", props)
        params.put("required", JSONArray().put(paramName))
        obj.put("parameters", params)
        return obj
    }

    private fun buildSmsToolDef(): JSONObject {
        val obj = JSONObject()
        obj.put("name", "send_sms")
        obj.put("description", "Compose or send an SMS text message to a contact or number")
        val params = JSONObject()
        params.put("type", "OBJECT")
        val props = JSONObject()
        props.put("phone_number", JSONObject().put("type", "STRING").put("description", "Recipient phone number or name"))
        props.put("message", JSONObject().put("type", "STRING").put("description", "Message body to send"))
        params.put("properties", props)
        val req = JSONArray()
        req.put("phone_number")
        req.put("message")
        params.put("required", req)
        obj.put("parameters", params)
        return obj
    }

    private fun buildEmailToolDef(): JSONObject {
        val obj = JSONObject()
        obj.put("name", "send_email")
        obj.put("description", "Compose an email message to a recipient")
        val params = JSONObject()
        params.put("type", "OBJECT")
        val props = JSONObject()
        props.put("recipient", JSONObject().put("type", "STRING").put("description", "Recipient email address"))
        props.put("subject", JSONObject().put("type", "STRING").put("description", "Subject line"))
        props.put("body", JSONObject().put("type", "STRING").put("description", "Body content"))
        params.put("properties", props)
        val req = JSONArray()
        req.put("recipient")
        params.put("required", req)
        obj.put("parameters", params)
        return obj
    }

    private fun buildMemoryToolDef(): JSONObject {
        val obj = JSONObject()
        obj.put("name", "save_memory")
        obj.put("description", "Remember a preference, contact, or fact about the user in long-term storage")
        val params = JSONObject()
        params.put("type", "OBJECT")
        val props = JSONObject()
        props.put("key", JSONObject().put("type", "STRING").put("description", "Memory identifier, e.g. favorite_color"))
        props.put("value", JSONObject().put("type", "STRING").put("description", "Value to remember"))
        params.put("properties", props)
        val req = JSONArray()
        req.put("key")
        req.put("value")
        params.put("required", req)
        obj.put("parameters", params)
        return obj
    }

    private fun parseGeminiResponse(rawJson: String): AiResponse {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return AiResponse.Error("No response candidates")
            val candidate = candidates.optJSONObject(0) ?: return AiResponse.Error("Empty candidate")
            val content = candidate.optJSONObject("content") ?: return AiResponse.Error("No content")
            val parts = content.optJSONArray("parts") ?: return AiResponse.Error("No parts")

            var responseText = ""
            var toolCall: ToolCall? = null

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.has("text")) {
                    responseText += part.optString("text")
                }
                if (part.has("functionCall")) {
                    val fc = part.getJSONObject("functionCall")
                    val fnName = fc.getString("name")
                    val argsObj = fc.optJSONObject("args")
                    val argsMap = mutableMapOf<String, String>()
                    argsObj?.let { obj ->
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = obj.optString(key)
                        }
                    }
                    toolCall = ToolCall(name = fnName, arguments = argsMap)
                }
            }

            if (responseText.isBlank() && toolCall != null) {
                responseText = when (toolCall.name) {
                    "open_app" -> "Opening ${toolCall.arguments["app_name"] ?: "app"}, Sir."
                    "make_call" -> "Preparing to dial ${toolCall.arguments["phone_number"] ?: "contact"}, Sir. Shall I proceed?"
                    "send_sms" -> "I have drafted your message to ${toolCall.arguments["phone_number"]}: '${toolCall.arguments["message"]}'. Please confirm before sending."
                    "send_email" -> "Preparing email to ${toolCall.arguments["recipient"]}. Please confirm dispatch."
                    "set_alarm" -> "Setting an alarm for ${toolCall.arguments["time"]}, Sir."
                    "device_action" -> "Executing ${toolCall.arguments["action"]?.replace("_", " ")}, Sir."
                    "save_note" -> "I have archived that note into encrypted vault storage, Sir."
                    "save_memory" -> "Understood, Sir. I have committed that to long-term memory."
                    else -> "Understood, Sir. Processing your command."
                }
            }

            AiResponse.Success(replyText = responseText.trim(), toolCall = toolCall)
        } catch (e: Exception) {
            AiResponse.Error("Failed to parse AI response: ${e.message}")
        }
    }

    /**
     * Local Offline Fallback Rule-Engine:
     * When there is no internet or no API key, Jarvis remains functional for
     * automation, memory, questions, and system control.
     */
    fun processOfflineRuleEngine(query: String, history: List<ChatMessage> = emptyList()): AiResponse {
        val q = query.trim().lowercase()

        // 1. Time / Date queries
        if (q.contains("time is it") || q == "what time is it" || q.contains("current time")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return AiResponse.Success("The time is currently $time, Sir.")
        }
        if (q.contains("what is today") || q.contains("what date") || q.contains("what's today's date")) {
            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            return AiResponse.Success("Today is $date, Sir.")
        }

        // 2. Identity / Greeting
        if (q.contains("who are you") || q.contains("what are you") || q.contains("introduce yourself")) {
            return AiResponse.Success("I am JARVIS: Just A Rather Very Intelligent System. At your service, Sir.")
        }
        if (q.startsWith("hello") || q.startsWith("hi jarvis") || q == "jarvis" || q.startsWith("hey jarvis")) {
            return AiResponse.Success("Good day, Sir. All systems are operational. How may I be of assistance?")
        }

        // 3. Open App
        if (q.startsWith("open ") || q.startsWith("launch ") || q.startsWith("start ")) {
            val appName = q.replaceFirst("open ", "").replaceFirst("launch ", "").replaceFirst("start ", "").trim()
            return AiResponse.Success(
                replyText = "Opening $appName for you, Sir.",
                toolCall = ToolCall("open_app", mapOf("app_name" to appName))
            )
        }

        // 4. Phone Call
        if (q.startsWith("call ") || q.startsWith("dial ")) {
            val target = q.replaceFirst("call ", "").replaceFirst("dial ", "").trim()
            return AiResponse.Success(
                replyText = "Shall I place a call to $target, Sir? Please confirm.",
                toolCall = ToolCall("make_call", mapOf("phone_number" to target))
            )
        }

        // 5. Send Text / SMS
        if (q.contains("send text") || q.contains("send sms") || q.contains("message")) {
            // e.g. "send text to Dave I am on my way"
            val parts = query.split(Regex("(?i)to\\s+"), 2)
            val recipientAndBody = if (parts.size > 1) parts[1] else ""
            val spaceSplit = recipientAndBody.split(Regex("\\s+"), 2)
            val recipient = if (spaceSplit.isNotEmpty()) spaceSplit[0] else "Contact"
            val message = if (spaceSplit.size > 1) spaceSplit[1] else "Hello from Jarvis"

            return AiResponse.Success(
                replyText = "Drafted SMS to $recipient: \"$message\". Confirm dispatch, Sir?",
                toolCall = ToolCall("send_sms", mapOf("phone_number" to recipient, "message" to message))
            )
        }

        // 6. Email
        if (q.contains("send email") || q.contains("compose email") || q.startsWith("email ")) {
            return AiResponse.Success(
                replyText = "Preparing email client, Sir.",
                toolCall = ToolCall("send_email", mapOf("recipient" to "", "subject" to "Note from Jarvis", "body" to query))
            )
        }

        // 7. Alarms & Timers
        if (q.contains("set alarm") || q.contains("wake me up")) {
            val timeMatch = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)").find(q)?.value ?: "07:00"
            return AiResponse.Success(
                replyText = "Setting an alarm for $timeMatch, Sir.",
                toolCall = ToolCall("set_alarm", mapOf("time" to timeMatch, "label" to "Jarvis Alarm"))
            )
        }

        // 8. Device Navigation (Accessibility)
        if (q.contains("go home") || q == "home screen") {
            return AiResponse.Success(
                replyText = "Navigating to home screen, Sir.",
                toolCall = ToolCall("device_action", mapOf("action" to "go_home"))
            )
        }
        if (q.contains("go back") || q == "back") {
            return AiResponse.Success(
                replyText = "Returning back, Sir.",
                toolCall = ToolCall("device_action", mapOf("action" to "go_back"))
            )
        }
        if (q.contains("recent apps") || q.contains("overview")) {
            return AiResponse.Success(
                replyText = "Displaying active tasks, Sir.",
                toolCall = ToolCall("device_action", mapOf("action" to "show_recents"))
            )
        }
        if (q.contains("notifications") || q.contains("notification shade")) {
            return AiResponse.Success(
                replyText = "Opening notifications panel, Sir.",
                toolCall = ToolCall("device_action", mapOf("action" to "open_notifications"))
            )
        }

        // 9. Notes & Memories
        if (q.startsWith("note ") || q.startsWith("take a note") || q.startsWith("write down")) {
            val content = query.replaceFirst(Regex("(?i)^(note|take a note|write down)\\s*"), "").trim()
            return AiResponse.Success(
                replyText = "Note archived to encrypted vault: \"$content\".",
                toolCall = ToolCall("save_note", mapOf("content" to content))
            )
        }
        if (q.startsWith("remember that ") || q.startsWith("remember ")) {
            val fact = query.replaceFirst(Regex("(?i)^remember( that)?\\s*"), "").trim()
            return AiResponse.Success(
                replyText = "I have stored that in my persistent memory banks, Sir: \"$fact\".",
                toolCall = ToolCall("save_memory", mapOf("key" to "fact_${System.currentTimeMillis() % 10000}", "value" to fact))
            )
        }

        // Check if follow-up refers to recent conversation turn
        val lastTurn = history.lastOrNull { it.role == MessageRole.ASSISTANT }
        if (lastTurn != null && (q.contains("yes") || q.contains("proceed") || q.contains("confirm") || q.contains("do it"))) {
            return AiResponse.Success("Understood, Sir. Proceeding with the requested action.")
        }

        // General smart response
        return AiResponse.Success("I have noted that, Sir: \"$query\". Is there anything specific you would like me to automate or execute?")
    }
}
