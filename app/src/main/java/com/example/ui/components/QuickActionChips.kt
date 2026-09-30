package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary

data class QuickActionItem(
    val label: String,
    val command: String,
    val icon: ImageVector
)

val defaultQuickActions = listOf(
    QuickActionItem("Open Camera", "Open Camera", Icons.Default.CameraAlt),
    QuickActionItem("Send Message", "Send message to friend I will be there soon", Icons.AutoMirrored.Filled.Message),
    QuickActionItem("Make Call", "Call Tony", Icons.Default.Call),
    QuickActionItem("Set Alarm", "Set alarm for 7:00 AM", Icons.Default.Alarm),
    QuickActionItem("Take Note", "Note: Check Iron Man suit diagnostics at 4 PM", Icons.Default.NoteAlt),
    QuickActionItem("Read Notifications", "Read my unread notifications", Icons.Default.Notifications),
    QuickActionItem("Go Home", "Go to home screen", Icons.Default.Home)
)

@Composable
fun QuickActionChips(
    modifier: Modifier = Modifier,
    actions: List<QuickActionItem> = defaultQuickActions,
    onActionClick: (String) -> Unit
) {
    LazyRow(
        modifier = modifier.testTag("quick_action_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JarvisSurfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(20.dp))
                    .clickable { onActionClick(action.command) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("chip_${action.label.lowercase().replace(" ", "_")}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = ArcReactorCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = action.label,
                        color = JarvisTextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
