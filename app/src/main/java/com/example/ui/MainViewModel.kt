package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.ai.AiResponse
import com.example.ai.GeminiClient
import com.example.ai.JarvisPersonality
import com.example.auth.BiometricAuthManager
import com.example.automation.ActionType
import com.example.automation.DeviceAutomationManager
import com.example.automation.JarvisAccessibilityService
import com.example.automation.JarvisNotificationListenerService
import com.example.automation.PendingAction
import com.example.data.preferences.RetentionPeriod
import com.example.data.repository.ActionStatus
import com.example.data.repository.ChatMessage
import com.example.data.repository.MessageRole
import com.example.data.repository.UserMemory
import com.example.data.repository.UserNote
import com.example.data.repository.UserReminder
import com.example.voice.JarvisWakeWordService
import com.example.voice.SpeechManager
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class JarvisUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioEnergy: Float = 0f,
    val partialVoiceText: String = "",
    val statusMessage: String? = null,
    val isLoadingAi: Boolean = false,
    val pendingAction: PendingAction? = null,
    val wakeWordEnabled: Boolean = true,
    val wakeWordPhrase: String = "Hey Jarvis",
    val continuousListening: Boolean = false,
    val retentionPeriod: RetentionPeriod = RetentionPeriod.SEVEN_DAYS,
    val biometricLockEnabled: Boolean = false,
    val isBiometricAuthenticated: Boolean = false,
    val userHonorific: String = "Sir",
    val userName: String = "Tony",
    val customApiKey: String = "",
    val voicePitch: Float = 0.95f,
    val voiceRate: Float = 1.05f,
    val memories: List<UserMemory> = emptyList(),
    val notes: List<UserNote> = emptyList(),
    val reminders: List<UserReminder> = emptyList(),
    val isAccessibilityActive: Boolean = false,
    val isNotificationServiceActive: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as JarvisApplication
    private val chatRepo = app.chatRepository
    private val memoryRepo = app.memoryRepository
    private val prefs = app.preferences

    val speechManager = SpeechManager(application)
    val ttsManager = TextToSpeechManager(application)
    val wakeWordDetector = WakeWordDetector(application, viewModelScope)
    val automationManager = DeviceAutomationManager(application)
    val biometricManager = BiometricAuthManager(application)
    val geminiClient = GeminiClient()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    private val _isLoadingAi = MutableStateFlow(false)
    private val _isBiometricAuthenticated = MutableStateFlow(false)

    val uiState: StateFlow<JarvisUiState> = combine(
        chatRepo.allMessages,
        speechManager.isListening,
        ttsManager.isSpeaking,
        speechManager.rmsDb,
        speechManager.partialText,
        speechManager.statusMessage,
        _isLoadingAi,
        _pendingAction,
        _isBiometricAuthenticated,
        prefs.retentionPeriod,
        prefs.wakeWordEnabled,
        prefs.wakeWordPhrase,
        prefs.continuousListening,
        prefs.biometricLockEnabled,
        prefs.userHonorific,
        prefs.userName,
        prefs.customApiKey,
        prefs.voicePitch,
        prefs.voiceRate,
        memoryRepo.allMemories,
        memoryRepo.allNotes,
        memoryRepo.allReminders
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val msgs = args[0] as List<ChatMessage>
        val isListen = args[1] as Boolean
        val isSpeak = args[2] as Boolean
        val rms = args[3] as Float
        val partial = args[4] as String
        val statusMsg = args[5] as? String
        val loading = args[6] as Boolean
        val pending = args[7] as? PendingAction
        val bioAuth = args[8] as Boolean
        val retention = args[9] as RetentionPeriod
        val wakeWord = args[10] as Boolean
        val phrase = args[11] as String
        val contListen = args[12] as Boolean
        val bioLock = args[13] as Boolean
        val honorific = args[14] as String
        val name = args[15] as String
        val customKey = args[16] as String
        val pitch = args[17] as Float
        val rate = args[18] as Float
        val memories = args[19] as List<UserMemory>
        val notes = args[20] as List<UserNote>
        val reminders = args[21] as List<UserReminder>

        JarvisUiState(
            messages = msgs,
            isListening = isListen,
            isSpeaking = isSpeak,
            audioEnergy = rms,
            partialVoiceText = partial,
            statusMessage = statusMsg,
            isLoadingAi = loading,
            pendingAction = pending,
            isBiometricAuthenticated = bioAuth,
            retentionPeriod = retention,
            wakeWordEnabled = wakeWord,
            wakeWordPhrase = phrase,
            continuousListening = contListen,
            biometricLockEnabled = bioLock,
            userHonorific = honorific,
            userName = name,
            customApiKey = customKey,
            voicePitch = pitch,
            voiceRate = rate,
            memories = memories,
            notes = notes,
            reminders = reminders,
            isAccessibilityActive = JarvisAccessibilityService.isConnected(),
            isNotificationServiceActive = JarvisNotificationListenerService.isConnected()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JarvisUiState()
    )

    init {
        // Apply voice parameters
        viewModelScope.launch {
            val pitch = prefs.voicePitch.first()
            val rate = prefs.voiceRate.first()
            ttsManager.setVoiceParams(pitch, rate)

            // Setup wake-word detector
            val wakePhrase = prefs.wakeWordPhrase.first()
            wakeWordDetector.setWakeWord(wakePhrase)

            // If biometric lock is disabled, mark as authenticated by default
            val bioLock = prefs.biometricLockEnabled.first()
            if (!bioLock) {
                _isBiometricAuthenticated.value = true
            }

            // Prune expired messages on launch
            val retention = prefs.retentionPeriod.first()
            chatRepo.pruneExpiredMessages(retention)

            // If messages are completely empty, greet the user
            val recent = chatRepo.getRecentContext(1)
            if (recent.isEmpty()) {
                val honorific = prefs.userHonorific.first()
                val greeting = "All systems online and operational, $honorific. I am Jarvis, your personal assistant. How may I serve you today?"
                chatRepo.addMessage(MessageRole.ASSISTANT, greeting)
            }

            // Activate wake word detector if enabled and permitted
            checkAndStartWakeWord()
        }
    }

    fun checkAndStartWakeWord() {
        viewModelScope.launch {
            val enabled = prefs.wakeWordEnabled.first()
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (enabled && hasPermission && !speechManager.isListening.value && !wakeWordDetector.isDetecting.value) {
                wakeWordDetector.startDetection {
                    triggerWakeWordDetected()
                }
            }
        }
    }

    fun authenticateWithBiometrics(activity: FragmentActivity) {
        if (!_isBiometricAuthenticated.value) {
            biometricManager.authenticate(
                activity = activity,
                title = "Jarvis Security Clearance",
                subtitle = "Identify yourself to access the Jarvis interface",
                onSuccess = {
                    _isBiometricAuthenticated.value = true
                },
                onError = { err ->
                    // User error or cancelled; keep locked until verified
                }
            )
        }
    }

    fun toggleVoiceListening() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            checkAndStartWakeWord()
        } else {
            wakeWordDetector.stopDetection()
            ttsManager.stop()
            val cont = uiState.value.continuousListening
            viewModelScope.launch {
                // Brief pause to release hardware microphone session before speech recognition opens
                delay(60)
                speechManager.startListening(continuous = cont) { recognizedText ->
                    if (recognizedText.isNotBlank()) {
                        sendMessage(recognizedText)
                    }
                }
            }
        }
    }

    fun triggerWakeWordDetected() {
        ttsManager.stop()
        wakeWordDetector.stopDetection()
        viewModelScope.launch {
            // Brief pause to release microphone before continuous speech listening engages
            delay(60)
            speechManager.startListening(continuous = true) { spoken ->
                if (spoken.isNotBlank()) {
                    sendMessage(spoken)
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentHonorific = uiState.value.userHonorific
        val currentName = uiState.value.userName
        val customKey = uiState.value.customApiKey

        viewModelScope.launch {
            // 1. Add User message to local encrypted DB
            chatRepo.addMessage(MessageRole.USER, userText)
            _isLoadingAi.value = true

            // 2. Query Memory for prompt context
            val memorySummary = memoryRepo.getMemorySummaryForPrompt()

            // 3. Retrieve recent conversation history for multi-turn context
            val history = chatRepo.getRecentContext(10)

            // 4. Build system instruction
            val sysInstruction = JarvisPersonality.buildSystemInstruction(
                userHonorific = currentHonorific,
                userName = currentName,
                memorySummary = memorySummary,
                accessibilityConnected = JarvisAccessibilityService.isConnected(),
                notificationListenerConnected = JarvisNotificationListenerService.isConnected()
            )

            // 5. Query Gemini / Local Offline Engine
            val response = geminiClient.generateResponse(
                prompt = userText,
                history = history,
                systemInstruction = sysInstruction,
                customApiKey = customKey
            )

            _isLoadingAi.value = false

            when (response) {
                is AiResponse.Success -> {
                    // Check if AI requested a tool/action
                    val toolCall = response.toolCall
                    if (toolCall != null) {
                        handleToolCall(toolCall, response.replyText)
                    } else {
                        chatRepo.addMessage(MessageRole.ASSISTANT, response.replyText)
                        if (prefs.autoSpeak.first()) {
                            ttsManager.speak(response.replyText)
                        }
                    }
                }
                is AiResponse.Error -> {
                    val fallbackMsg = "My apologies, $currentHonorific. I encountered an issue: ${response.message}"
                    chatRepo.addMessage(MessageRole.ASSISTANT, fallbackMsg)
                    ttsManager.speak(fallbackMsg)
                }
            }

            // If not in continuous listening mode, resume offline wake word detection
            if (!uiState.value.continuousListening) {
                checkAndStartWakeWord()
            }
        }
    }

    private suspend fun handleToolCall(tool: com.example.ai.ToolCall, replyText: String) {
        when (tool.name) {
            "open_app" -> {
                val appName = tool.arguments["app_name"].orEmpty()
                val pkg = automationManager.findAppByName(appName)
                if (pkg != null) {
                    automationManager.openApp(pkg)
                    val confirmation = "Launching $appName now, Sir."
                    chatRepo.addMessage(MessageRole.ASSISTANT, confirmation, actionType = "OPEN_APP", actionStatus = ActionStatus.EXECUTED)
                    ttsManager.speak(confirmation)
                } else {
                    val notFound = "I was unable to locate an application named '$appName' on your system, Sir."
                    chatRepo.addMessage(MessageRole.ASSISTANT, notFound)
                    ttsManager.speak(notFound)
                }
            }
            "make_call" -> {
                val target = tool.arguments["phone_number"].orEmpty()
                // Require user confirmation for sensitive phone call!
                val pending = PendingAction(
                    type = ActionType.CALL,
                    title = "Initiate Phone Call",
                    description = "Call $target via device telephony",
                    target = target,
                    extraData = mapOf("number" to target)
                )
                _pendingAction.value = pending
                chatRepo.addMessage(
                    role = MessageRole.ASSISTANT,
                    content = replyText.ifBlank { "Shall I proceed with placing a call to $target, Sir?" },
                    actionType = "CALL",
                    actionPayload = target,
                    actionStatus = ActionStatus.PENDING_CONFIRMATION
                )
                ttsManager.speak(replyText)
            }
            "send_sms" -> {
                val target = tool.arguments["phone_number"].orEmpty()
                val msg = tool.arguments["message"].orEmpty()
                // Require user confirmation before dispatching SMS!
                val pending = PendingAction(
                    type = ActionType.SMS,
                    title = "Send SMS Text Message",
                    description = "\"$msg\"",
                    target = target,
                    extraData = mapOf("number" to target, "message" to msg)
                )
                _pendingAction.value = pending
                chatRepo.addMessage(
                    role = MessageRole.ASSISTANT,
                    content = replyText.ifBlank { "Drafted text to $target: '$msg'. Ready to dispatch upon your confirmation, Sir." },
                    actionType = "SMS",
                    actionPayload = "$target: $msg",
                    actionStatus = ActionStatus.PENDING_CONFIRMATION
                )
                ttsManager.speak(replyText)
            }
            "send_email" -> {
                val recipient = tool.arguments["recipient"].orEmpty()
                val subject = tool.arguments["subject"].orEmpty()
                val body = tool.arguments["body"].orEmpty()
                val pending = PendingAction(
                    type = ActionType.EMAIL,
                    title = "Compose Email",
                    description = "Subject: $subject\n$body",
                    target = recipient,
                    extraData = mapOf("recipient" to recipient, "subject" to subject, "body" to body)
                )
                _pendingAction.value = pending
                chatRepo.addMessage(
                    role = MessageRole.ASSISTANT,
                    content = replyText.ifBlank { "Email prepared for $recipient. Awaiting authorization, Sir." },
                    actionType = "EMAIL",
                    actionPayload = recipient,
                    actionStatus = ActionStatus.PENDING_CONFIRMATION
                )
                ttsManager.speak(replyText)
            }
            "set_alarm" -> {
                val timeStr = tool.arguments["time"].orEmpty()
                var hour = 7
                var minute = 0
                val match = Regex("(\\d{1,2}):?(\\d{2})?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(timeStr)
                if (match != null) {
                    val rawHour = match.groupValues[1].toIntOrNull() ?: 7
                    val rawMin = match.groupValues[2].toIntOrNull() ?: 0
                    val ampm = match.groupValues[3].lowercase()
                    hour = when {
                        ampm == "pm" && rawHour < 12 -> rawHour + 12
                        ampm == "am" && rawHour == 12 -> 0
                        else -> rawHour
                    }
                    minute = rawMin
                }
                automationManager.setAlarm(hour, minute, "Jarvis Alarm")
                val response = "Alarm set for %02d:%02d, Sir.".format(hour, minute)
                chatRepo.addMessage(MessageRole.ASSISTANT, response, actionType = "ALARM", actionStatus = ActionStatus.EXECUTED)
                ttsManager.speak(response)
            }
            "device_action" -> {
                val actionName = tool.arguments["action"].orEmpty()
                val executed = when (actionName) {
                    "go_home" -> JarvisAccessibilityService.goHome()
                    "go_back" -> JarvisAccessibilityService.goBack()
                    "show_recents" -> JarvisAccessibilityService.showRecents()
                    "open_notifications" -> JarvisAccessibilityService.openNotifications()
                    "open_quick_settings" -> JarvisAccessibilityService.openQuickSettings()
                    "lock_screen" -> JarvisAccessibilityService.lockScreen()
                    else -> false
                }
                val msg = if (executed) {
                    "Action $actionName executed successfully, Sir."
                } else {
                    "Accessibility action requested: $actionName. Please ensure Jarvis Accessibility Service is enabled in Android Settings."
                }
                chatRepo.addMessage(MessageRole.ASSISTANT, msg, actionType = "ACCESSIBILITY", actionStatus = ActionStatus.EXECUTED)
                ttsManager.speak(msg)
            }
            "save_note" -> {
                val content = tool.arguments["content"].orEmpty()
                memoryRepo.saveNote("Quick Note", content)
                val msg = "Note saved to your encrypted local vault, Sir."
                chatRepo.addMessage(MessageRole.ASSISTANT, msg, actionType = "NOTE", actionStatus = ActionStatus.EXECUTED)
                ttsManager.speak(msg)
            }
            "save_memory" -> {
                val key = tool.arguments["key"].orEmpty()
                val value = tool.arguments["value"].orEmpty()
                memoryRepo.saveMemory("PREFERENCE", key, value)
                val msg = "Committed to permanent memory banks, Sir: $key = $value."
                chatRepo.addMessage(MessageRole.ASSISTANT, msg)
                ttsManager.speak(msg)
            }
            else -> {
                chatRepo.addMessage(MessageRole.ASSISTANT, replyText)
                ttsManager.speak(replyText)
            }
        }
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        viewModelScope.launch {
            when (action.type) {
                ActionType.CALL -> {
                    val num = action.extraData["number"] ?: action.target
                    automationManager.executeCall(num, directCall = false)
                }
                ActionType.SMS -> {
                    val num = action.extraData["number"] ?: action.target
                    val msg = action.extraData["message"] ?: ""
                    automationManager.executeSms(num, msg)
                }
                ActionType.EMAIL -> {
                    val recipient = action.extraData["recipient"] ?: action.target
                    val subject = action.extraData["subject"] ?: "Jarvis Dispatch"
                    val body = action.extraData["body"] ?: ""
                    automationManager.executeEmail(recipient, subject, body)
                }
                else -> {}
            }
            chatRepo.addMessage(
                MessageRole.SYSTEM,
                "Authorized: ${action.title} executed."
            )
            _pendingAction.value = null
        }
    }

    fun dismissPendingAction() {
        val action = _pendingAction.value ?: return
        viewModelScope.launch {
            chatRepo.addMessage(
                MessageRole.SYSTEM,
                "Cancelled: ${action.title} was declined by user."
            )
            _pendingAction.value = null
        }
    }

    fun setRetentionPeriod(period: RetentionPeriod) {
        viewModelScope.launch {
            prefs.setRetentionPeriod(period)
            chatRepo.pruneExpiredMessages(period)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setWakeWordEnabled(enabled)
            if (enabled) {
                checkAndStartWakeWord()
                JarvisWakeWordService.start(getApplication())
            } else {
                wakeWordDetector.stopDetection()
                JarvisWakeWordService.stop(getApplication())
            }
        }
    }

    fun setWakeWordPhrase(phrase: String) {
        viewModelScope.launch {
            prefs.setWakeWordPhrase(phrase)
            wakeWordDetector.setWakeWord(phrase)
        }
    }

    fun setContinuousListening(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setContinuousListening(enabled)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBiometricLock(enabled)
            if (!enabled) {
                _isBiometricAuthenticated.value = true
            }
        }
    }

    fun setUserHonorific(honorific: String) {
        viewModelScope.launch {
            prefs.setUserHonorific(honorific)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            prefs.setUserName(name)
        }
    }

    fun setCustomApiKey(key: String) {
        viewModelScope.launch {
            prefs.setCustomApiKey(key)
        }
    }

    fun setVoicePitch(pitch: Float) {
        viewModelScope.launch {
            prefs.setVoicePitch(pitch)
            ttsManager.setVoiceParams(pitch, uiState.value.voiceRate)
        }
    }

    fun setVoiceRate(rate: Float) {
        viewModelScope.launch {
            prefs.setVoiceRate(rate)
            ttsManager.setVoiceParams(uiState.value.voicePitch, rate)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepo.clearHistory()
            val honorific = uiState.value.userHonorific
            val msg = "Memory cache purged, $honorific. Ready for new inquiries."
            chatRepo.addMessage(MessageRole.ASSISTANT, msg)
        }
    }

    fun clearPersonalData() {
        viewModelScope.launch {
            memoryRepo.clearAllPersonalData()
        }
    }

    fun addManualNote(title: String, content: String) {
        viewModelScope.launch {
            memoryRepo.saveNote(title, content)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteNote(id)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteMemory(id)
        }
    }

    fun toggleReminder(id: Long, completed: Boolean) {
        viewModelScope.launch {
            memoryRepo.toggleReminder(id, completed)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteReminder(id)
        }
    }

    fun exportPersonalData(): String {
        val state = uiState.value
        val sb = StringBuilder("=== JARVIS SECURE DATA EXPORT ===\n")
        sb.append("Generated: ${java.util.Date()}\n\n")
        sb.append("--- MEMORIES (${state.memories.size}) ---\n")
        state.memories.forEach {
            sb.append("[${it.category}] ${it.key}: ${it.value}\n")
        }
        sb.append("\n--- PERSONAL NOTES (${state.notes.size}) ---\n")
        state.notes.forEach {
            sb.append("Note: ${it.title} - ${it.content}\n")
        }
        sb.append("\n--- RECENT CONVERSATIONS (${state.messages.size}) ---\n")
        state.messages.forEach {
            sb.append("[${it.role}] ${it.content}\n")
        }
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
        wakeWordDetector.stopDetection()
    }
}
