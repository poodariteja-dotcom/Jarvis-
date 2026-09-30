package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.DeviceAutomationManager
import com.example.automation.JarvisAccessibilityService
import com.example.ui.JarvisUiState
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorPlasma
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun AutomationScreen(
    state: JarvisUiState,
    automationManager: DeviceAutomationManager,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("automation_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "AUTOMATION PROTOCOLS",
                color = JarvisTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Hardware Integration & System Services",
                color = ArcReactorCyan,
                fontSize = 11.sp
            )
        }

        // Accessibility Service Card
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
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = if (state.isAccessibilityActive) ArcReactorCyan else ArcReactorGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accessibility Automation Service",
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        ServiceStatusBadge(isActive = state.isAccessibilityActive)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows Jarvis to perform device actions: navigate home, back, switch apps, open notification shade, or execute requested gestures.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { automationManager.openSystemSettings("accessibility") },
                            colors = ButtonDefaults.buttonColors(containerColor = ArcReactorCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Configure in Android Settings", color = JarvisBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Direct test buttons if active
                    if (state.isAccessibilityActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { JarvisAccessibilityService.goHome() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Home", fontSize = 11.sp, color = ArcReactorCyan)
                            }
                            OutlinedButton(
                                onClick = { JarvisAccessibilityService.openNotifications() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Notifications", fontSize = 11.sp, color = ArcReactorCyan)
                            }
                        }
                    }
                }
            }
        }

        // Notification Listener Service Card
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
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (state.isNotificationServiceActive) ArcReactorCyan else ArcReactorGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notification Listener Service",
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        ServiceStatusBadge(isActive = state.isNotificationServiceActive)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows Jarvis to read incoming notifications and summarize emails, messages, and alerts via voice briefing.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { automationManager.openSystemSettings("notifications") },
                        colors = ButtonDefaults.buttonColors(containerColor = ArcReactorCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Notification Access", color = JarvisBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Automation Test Triggers
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Voice Automation Triggers",
                        color = JarvisTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AutomationTriggerRow("Open Camera", "Launches device camera", Icons.Default.Apps) {
                        onExecuteCommand("Open Camera")
                    }
                    AutomationTriggerRow("Call Contact", "Prepares call with confirmation", Icons.Default.Call) {
                        onExecuteCommand("Call Tony")
                    }
                    AutomationTriggerRow("Compose Message", "Drafts SMS with confirmation", Icons.AutoMirrored.Filled.Message) {
                        onExecuteCommand("Send message to friend I am on my way")
                    }
                    AutomationTriggerRow("Set Alarm", "Programs alarm clock for 7 AM", Icons.Default.Alarm) {
                        onExecuteCommand("Set alarm for 7:00 AM")
                    }
                    AutomationTriggerRow("Wi-Fi Settings", "Opens device connectivity panel", Icons.Default.Settings) {
                        automationManager.openSystemSettings("wifi")
                    }
                }
            }
        }

        // Honest Android Limitations Disclosure Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, ArcReactorGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ArcReactorGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Platform Architecture & Safeguards",
                            color = ArcReactorGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Android Sandbox: Third-party apps are isolated; Jarvis uses standard Android Intents and the dedicated AccessibilityService.\n" +
                                "• Zero Silent Dispatch: Calls, SMS, and emails require explicit user confirmation via dialog.\n" +
                                "• Play Store Policy: AccessibilityService is governed strictly by user consent. Direct APK installation provides unrestricted assistant features.",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusBadge(isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) ArcReactorCyan.copy(alpha = 0.15f) else JarvisTextDim.copy(alpha = 0.2f))
            .border(
                1.dp,
                if (isActive) ArcReactorCyan else JarvisBorder,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isActive) "ACTIVE" else "STANDBY",
            color = if (isActive) ArcReactorCyan else JarvisTextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AutomationTriggerRow(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ArcReactorPlasma.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ArcReactorCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = desc, color = JarvisTextDim, fontSize = 11.sp)
            }
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text("Trigger", fontSize = 11.sp, color = ArcReactorCyan)
        }
    }
}
