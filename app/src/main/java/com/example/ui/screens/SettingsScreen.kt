package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.RetentionPeriod
import com.example.ui.JarvisUiState
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsScreen(
    state: JarvisUiState,
    onSetWakeWordEnabled: (Boolean) -> Unit,
    onSetWakeWordPhrase: (String) -> Unit,
    onSetContinuousListening: (Boolean) -> Unit,
    onSetRetentionPeriod: (RetentionPeriod) -> Unit,
    onSetBiometricLock: (Boolean) -> Unit,
    onSetUserHonorific: (String) -> Unit,
    onSetUserName: (String) -> Unit,
    onSetCustomApiKey: (String) -> Unit,
    onSetVoicePitch: (Float) -> Unit,
    onSetVoiceRate: (Float) -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember(state.customApiKey) { mutableStateOf(state.customApiKey) }
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "JARVIS CONFIGURATION",
                color = JarvisTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Voice, Memory, Biometrics & Security",
                color = ArcReactorCyan,
                fontSize = 11.sp
            )
        }

        // Voice & Wake Word Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hearing, contentDescription = null, tint = ArcReactorCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Wake-Word & Continuous Listening", color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word Detection", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Listens offline for trigger phrase", color = JarvisTextDim, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.wakeWordEnabled,
                            onCheckedChange = onSetWakeWordEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = ArcReactorCyan),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    if (state.wakeWordEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Trigger Phrase:", color = JarvisTextSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Hey Jarvis", "Jarvis", "Computer").forEach { phrase ->
                                val isSelected = state.wakeWordPhrase == phrase
                                OutlinedButton(
                                    onClick = { onSetWakeWordPhrase(phrase) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) ArcReactorCyan.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                                        contentColor = if (isSelected) ArcReactorCyan else JarvisTextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(phrase, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Listening Mode", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Maintains mic active for multi-turn commands", color = JarvisTextDim, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.continuousListening,
                            onCheckedChange = onSetContinuousListening,
                            colors = SwitchDefaults.colors(checkedThumbColor = ArcReactorCyan),
                            modifier = Modifier.testTag("continuous_listening_switch")
                        )
                    }
                }
            }
        }

        // Voice Synthesis Pitch & Rate
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = ArcReactorCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "TTS Vocal Modulation", color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = onTestVoice,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = ArcReactorCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Voice", fontSize = 11.sp, color = ArcReactorCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pitch: ${String.format("%.2f", state.voicePitch)}x", color = JarvisTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = state.voicePitch,
                        onValueChange = onSetVoicePitch,
                        valueRange = 0.6f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = ArcReactorCyan, activeTrackColor = ArcReactorCyan)
                    )

                    Text("Speech Rate: ${String.format("%.2f", state.voiceRate)}x", color = JarvisTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = state.voiceRate,
                        onValueChange = onSetVoiceRate,
                        valueRange = 0.7f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = ArcReactorCyan, activeTrackColor = ArcReactorCyan)
                    )
                }
            }
        }

        // Biometrics & App Security
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ArcReactorGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Biometric Clearance Lock", color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Require fingerprint, facial scan, or device PIN/password via BiometricPrompt upon launching Jarvis.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Access Lock", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = state.biometricLockEnabled,
                            onCheckedChange = onSetBiometricLock,
                            colors = SwitchDefaults.colors(checkedThumbColor = ArcReactorGold),
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }
                }
            }
        }

        // User Honorific & Persona Customization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ArcReactorCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Persona & Addressing", color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Preferred Honorific:", color = JarvisTextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("Sir", "Ma'am", "Boss").forEach { hon ->
                            val isSelected = state.userHonorific == hon
                            OutlinedButton(
                                onClick = { onSetUserHonorific(hon) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) ArcReactorCyan.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = if (isSelected) ArcReactorCyan else JarvisTextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(hon, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            onSetUserName(it)
                        },
                        label = { Text("Your Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcReactorCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Custom API Key Override
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = ArcReactorCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Custom LLM API Key (Optional)", color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jarvis automatically uses the AI Studio injected Gemini key or local offline intelligence. You can optionally provide a custom key below.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            onSetCustomApiKey(it)
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcReactorCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                }
            }
        }
    }
}
