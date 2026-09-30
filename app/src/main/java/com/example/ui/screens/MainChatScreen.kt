package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatMessage
import com.example.data.repository.MessageRole
import com.example.ui.JarvisUiState
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.ArcReactorOrb
import com.example.ui.components.AudioAmplitudeVisualizer
import com.example.ui.components.QuickActionChips
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorPlasma
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainChatScreen(
    state: JarvisUiState,
    onSendMessage: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onReplayAudio: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    // Action confirmation dialog if pending
    state.pendingAction?.let { pending ->
        ActionConfirmationDialog(
            action = pending,
            onConfirm = onConfirmAction,
            onDismiss = onDismissAction
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("main_chat_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HUD Status Header Bar
            HudStatusHeader(state = state)

            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("chat_message_list"),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onReplayAudio = { onReplayAudio(message.content) }
                    )
                }

                if (state.isLoadingAi) {
                    item {
                        JarvisLoadingIndicator()
                    }
                }
            }

            // Voice Orb & Live HUD Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(JarvisSurface.copy(alpha = 0.95f))
                    .border(
                        1.dp,
                        JarvisBorder,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real-time Audio Amplitude Waveform Visualizer
                if (state.isListening || state.isSpeaking) {
                    AudioAmplitudeVisualizer(
                        amplitude = state.audioEnergy,
                        isRecording = state.isListening || state.isSpeaking,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (state.partialVoiceText.isNotBlank()) {
                    Text(
                        text = "\"${state.partialVoiceText}\"",
                        color = ArcReactorCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Arc Reactor Voice Orb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ArcReactorOrb(
                        size = 90.dp,
                        isListening = state.isListening,
                        isSpeaking = state.isSpeaking,
                        audioEnergy = state.audioEnergy,
                        onClick = onToggleVoice
                    )
                }

                val statusText = when {
                    state.isListening -> if (state.partialVoiceText.isNotBlank()) "\"${state.partialVoiceText}\"" else (state.statusMessage ?: "Listening... Speak command")
                    state.isSpeaking -> "Jarvis Vocalizing..."
                    !state.statusMessage.isNullOrBlank() -> state.statusMessage
                    state.wakeWordEnabled -> "Tap orb or say \"${state.wakeWordPhrase}\""
                    else -> "Tap orb to speak"
                }

                Text(
                    text = statusText,
                    color = when {
                        state.isListening -> ArcReactorCyan
                        state.isSpeaking -> ArcReactorPlasma
                        !state.statusMessage.isNullOrBlank() -> ArcReactorGold
                        else -> JarvisTextDim
                    },
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // Quick Action Chips
                QuickActionChips(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    onActionClick = { cmd -> onSendMessage(cmd) }
                )

                // Text Input Bar Fallback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        placeholder = {
                            Text(
                                "Ask Jarvis or enter command...",
                                color = JarvisTextDim,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcReactorCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            cursorColor = ArcReactorCyan
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                            } else {
                                onToggleVoice()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ArcReactorCyan)
                            .testTag("send_or_mic_button")
                    ) {
                        Icon(
                            imageVector = if (textInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if (textInput.isNotBlank()) "Send" else "Voice",
                            tint = JarvisBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudStatusHeader(state: JarvisUiState) {
    Surface(
        color = JarvisSurface.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // System Name & Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ArcReactorCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "JARVIS CORE 4.0",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SYSTEM READY • ${state.retentionPeriod.label} MEMORY",
                        color = JarvisTextDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Badges for features
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.wakeWordEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ArcReactorCyan.copy(alpha = 0.12f))
                            .border(1.dp, ArcReactorCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WAKE: ON",
                            color = ArcReactorCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.biometricLockEnabled) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Encrypted Vault",
                        tint = ArcReactorGold,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onReplayAudio: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val isSystem = message.role == MessageRole.SYSTEM

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "• ${message.content} •",
                color = JarvisTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Jarvis Avatar Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ArcReactorCyan.copy(alpha = 0.15f))
                    .border(1.dp, ArcReactorCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = ArcReactorCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) ArcReactorPlasma.copy(alpha = 0.85f)
                        else JarvisSurfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isUser) ArcReactorCyan.copy(alpha = 0.5f) else JarvisBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = timeFormatted,
                                color = JarvisTextDim,
                                fontSize = 10.sp
                            )
                            IconButton(
                                onClick = onReplayAudio,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak response",
                                    tint = ArcReactorCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = timeFormatted,
                            color = JarvisTextSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ArcReactorPlasma)
                    .border(1.dp, ArcReactorCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "YOU",
                    color = JarvisTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun JarvisLoadingIndicator() {
    Row(
        modifier = Modifier.padding(start = 40.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            color = ArcReactorCyan,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Text(
            text = "Jarvis processing neural pathways...",
            color = ArcReactorCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
