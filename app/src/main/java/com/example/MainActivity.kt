package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.MainChatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorPlasma
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisTheme

enum class JarvisNavTab(val label: String, val icon: ImageVector) {
    ASSISTANT("Assistant", Icons.Default.GraphicEq),
    MEMORY("Memory Vault", Icons.Default.Storage),
    AUTOMATION("Automation", Icons.Default.AccessibilityNew),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : FragmentActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }

    private var viewModelRef: MainViewModel? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val audioIndex = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
            if (audioIndex != -1 && grantResults.getOrNull(audioIndex) == PackageManager.PERMISSION_GRANTED) {
                viewModelRef?.checkAndStartWakeWord()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JarvisTheme(darkTheme = true) {
                val viewModel: MainViewModel = viewModel()
                viewModelRef = viewModel
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var selectedNavIndex by remember { mutableIntStateOf(0) }
                var showOnboarding by remember { mutableStateOf(false) }

                fun requestRequiredPermissions() {
                    val permissions = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_CONTACTS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        permissions.toTypedArray(),
                        REQUEST_CODE_PERMISSIONS
                    )
                }

                // Check biometric lock on launch
                LaunchedEffect(state.biometricLockEnabled) {
                    if (state.biometricLockEnabled && !state.isBiometricAuthenticated) {
                        viewModel.authenticateWithBiometrics(this@MainActivity)
                    }
                }

                // Check first-time onboarding
                LaunchedEffect(Unit) {
                    val app = application as JarvisApplication
                    val completed = app.preferences.onboardingCompleted.first()
                    if (!completed) {
                        showOnboarding = true
                    } else {
                        viewModel.checkAndStartWakeWord()
                    }
                }

                // BackHandler returns to Assistant screen if on a sub-screen
                BackHandler(enabled = selectedNavIndex != 0) {
                    selectedNavIndex = 0
                }

                // If Biometric Lock is enabled and not yet authenticated, show security gate
                if (state.biometricLockEnabled && !state.isBiometricAuthenticated) {
                    BiometricLockScreen(
                        onAuthenticate = { viewModel.authenticateWithBiometrics(this@MainActivity) }
                    )
                } else if (showOnboarding) {
                    OnboardingScreen(
                        onRequestPermissions = { requestRequiredPermissions() },
                        onCompleteOnboarding = {
                            lifecycleScope.launch {
                                val app = application as JarvisApplication
                                app.preferences.setOnboardingCompleted(true)
                            }
                            showOnboarding = false
                            viewModel.checkAndStartWakeWord()
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = JarvisBackground,
                        bottomBar = {
                            JarvisBottomNavigation(
                                selectedIndex = selectedNavIndex,
                                onSelectIndex = { selectedNavIndex = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedNavIndex) {
                                0 -> MainChatScreen(
                                    state = state,
                                    onSendMessage = { viewModel.sendMessage(it) },
                                    onToggleVoice = {
                                        if (ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            viewModel.toggleVoiceListening()
                                        } else {
                                            requestRequiredPermissions()
                                        }
                                    },
                                    onReplayAudio = { viewModel.ttsManager.speak(it) },
                                    onConfirmAction = { viewModel.confirmPendingAction() },
                                    onDismissAction = { viewModel.dismissPendingAction() }
                                )
                                1 -> MemoryScreen(
                                    state = state,
                                    onSetRetention = { viewModel.setRetentionPeriod(it) },
                                    onClearChatHistory = { viewModel.clearChatHistory() },
                                    onClearAllData = { viewModel.clearPersonalData() },
                                    onAddNote = { t, c -> viewModel.addManualNote(t, c) },
                                    onDeleteNote = { viewModel.deleteNote(it) },
                                    onDeleteMemory = { viewModel.deleteMemory(it) },
                                    onToggleReminder = { id, comp -> viewModel.toggleReminder(id, comp) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                    onExportData = { viewModel.exportPersonalData() }
                                )
                                2 -> AutomationScreen(
                                    state = state,
                                    automationManager = viewModel.automationManager,
                                    onExecuteCommand = { cmd ->
                                        selectedNavIndex = 0
                                        viewModel.sendMessage(cmd)
                                    }
                                )
                                3 -> SettingsScreen(
                                    state = state,
                                    onSetWakeWordEnabled = { viewModel.setWakeWordEnabled(it) },
                                    onSetWakeWordPhrase = { viewModel.setWakeWordPhrase(it) },
                                    onSetContinuousListening = { viewModel.setContinuousListening(it) },
                                    onSetRetentionPeriod = { viewModel.setRetentionPeriod(it) },
                                    onSetBiometricLock = { viewModel.setBiometricLock(it) },
                                    onSetUserHonorific = { viewModel.setUserHonorific(it) },
                                    onSetUserName = { viewModel.setUserName(it) },
                                    onSetCustomApiKey = { viewModel.setCustomApiKey(it) },
                                    onSetVoicePitch = { viewModel.setVoicePitch(it) },
                                    onSetVoiceRate = { viewModel.setVoiceRate(it) },
                                    onTestVoice = {
                                        viewModel.ttsManager.speak("Systems operational, ${state.userHonorific}. Voice modulation calibrated.")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JarvisBottomNavigation(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("jarvis_bottom_navigation"),
        containerColor = JarvisSurface,
        tonalElevation = 8.dp
    ) {
        JarvisNavTab.entries.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectIndex(index) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = JarvisBackground,
                    selectedTextColor = ArcReactorCyan,
                    indicatorColor = ArcReactorCyan,
                    unselectedIconColor = JarvisTextDim,
                    unselectedTextColor = JarvisTextDim
                ),
                modifier = Modifier.testTag("nav_${tab.label.lowercase().replace(" ", "_")}")
            )
        }
    }
}

@Composable
private fun BiometricLockScreen(
    onAuthenticate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(24.dp)
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(ArcReactorGold.copy(alpha = 0.15f))
                    .border(2.dp, ArcReactorGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = ArcReactorGold,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "JARVIS VAULT ENCRYPTED",
                color = JarvisTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Biometric identity verification is required to decrypt conversation memory banks and access assistant controls.",
                color = JarvisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onAuthenticate,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
                    .testTag("biometric_authenticate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ArcReactorGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = JarvisBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify Clearance (PIN/Biometrics)",
                    color = JarvisBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
