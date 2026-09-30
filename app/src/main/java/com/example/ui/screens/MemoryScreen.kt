package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.RetentionPeriod
import com.example.ui.JarvisUiState
import com.example.ui.theme.ArcReactorCyan
import com.example.ui.theme.ArcReactorGold
import com.example.ui.theme.ArcReactorRed
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
fun MemoryScreen(
    state: JarvisUiState,
    onSetRetention: (RetentionPeriod) -> Unit,
    onClearChatHistory: () -> Unit,
    onClearAllData: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onExportData: () -> String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Memory & Retention", "Learned Facts", "Vault Notes", "Reminders")
    val context = LocalContext.current

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearAllDataDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("memory_screen")
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ENCRYPTED MEMORY VAULT",
                    color = JarvisTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hardware Keystore AES-256-GCM Protected",
                    color = ArcReactorCyan,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = {
                    val exportText = onExportData()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Jarvis Data Export", exportText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Encrypted vault export copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("export_data_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export data",
                    tint = ArcReactorCyan
                )
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = JarvisSurface,
            contentColor = ArcReactorCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) ArcReactorCyan else JarvisTextDim,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            when (selectedTab) {
                0 -> RetentionAndMemoryTab(
                    state = state,
                    onSetRetention = onSetRetention,
                    onClearHistoryClick = { showClearHistoryDialog = true },
                    onClearAllClick = { showClearAllDataDialog = true }
                )
                1 -> LearnedFactsTab(
                    memories = state.memories,
                    onDelete = onDeleteMemory
                )
                2 -> NotesVaultTab(
                    notes = state.notes,
                    onAddClick = { showAddNoteDialog = true },
                    onDelete = onDeleteNote
                )
                3 -> RemindersTab(
                    reminders = state.reminders,
                    onToggle = onToggleReminder,
                    onDelete = onDeleteReminder
                )
            }
        }
    }

    // Dialogs
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Purge Conversation Cache?") },
            text = { Text("This will permanently erase local chat memory turns. Context will be reset for new questions.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearChatHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcReactorRed)
                ) {
                    Text("Purge Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = JarvisSurface,
            titleContentColor = JarvisTextPrimary,
            textContentColor = JarvisTextSecondary
        )
    }

    if (showClearAllDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDataDialog = false },
            title = { Text("Erase All Personal Data?") },
            text = { Text("This will destroy all learned preferences, notes, reminders, and history stored in the local encrypted database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearAllDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcReactorRed)
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDataDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = JarvisSurface,
            titleContentColor = JarvisTextPrimary,
            textContentColor = JarvisTextSecondary
        )
    }

    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Encrypted Vault Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcReactorCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Content") },
                        modifier = Modifier.height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcReactorCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() || noteBody.isNotBlank()) {
                            onAddNote(noteTitle.ifBlank { "Untitled Note" }, noteBody)
                        }
                        showAddNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcReactorCyan)
                ) {
                    Text("Archive Note", color = JarvisBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = JarvisSurface,
            titleContentColor = JarvisTextPrimary,
            textContentColor = JarvisTextSecondary
        )
    }
}

@Composable
private fun RetentionAndMemoryTab(
    state: JarvisUiState,
    onSetRetention: (RetentionPeriod) -> Unit,
    onClearHistoryClick: () -> Unit,
    onClearAllClick: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = ArcReactorCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conversation Memory Retention",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configures how long multi-turn chat messages remain in local encrypted storage before auto-pruning. Stored messages provide conversational context for follow-up questions.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Retention options
                    RetentionPeriod.entries.forEach { period ->
                        val isSelected = state.retentionPeriod == period
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ArcReactorCyan.copy(alpha = 0.15f) else JarvisSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) ArcReactorCyan else JarvisBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("retention_${period.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = period.label,
                                        color = if (isSelected) ArcReactorCyan else JarvisTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = when (period) {
                                            RetentionPeriod.ONE_DAY -> "Strict ephemeral privacy, auto-purged daily"
                                            RetentionPeriod.SEVEN_DAYS -> "Balanced context across weekly routines"
                                            RetentionPeriod.THIRTY_DAYS -> "Deep conversational context for a month"
                                            RetentionPeriod.FOREVER -> "Never deleted until explicitly wiped"
                                        },
                                        color = JarvisTextDim,
                                        fontSize = 11.sp
                                    )
                                }
                                if (!isSelected) {
                                    OutlinedButton(
                                        onClick = { onSetRetention(period) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Select", fontSize = 11.sp, color = ArcReactorCyan)
                                    }
                                } else {
                                    Text(
                                        text = "ACTIVE",
                                        color = ArcReactorCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memory Metrics & Actions",
                        color = JarvisTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge(label = "Total Turns", value = state.messages.size.toString())
                        MetricBadge(label = "Learned Facts", value = state.memories.size.toString())
                        MetricBadge(label = "Vault Notes", value = state.notes.size.toString())
                        MetricBadge(label = "Reminders", value = state.reminders.size.toString())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearHistoryClick,
                            modifier = Modifier.weight(1f).testTag("clear_history_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcReactorGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear Chat Cache", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onClearAllClick,
                            modifier = Modifier.weight(1f).testTag("purge_all_data_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ArcReactorRed.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Purge All Vaults", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = value, color = ArcReactorCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, color = JarvisTextDim, fontSize = 10.sp)
    }
}

@Composable
private fun LearnedFactsTab(
    memories: List<com.example.data.repository.UserMemory>,
    onDelete: (Long) -> Unit
) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = JarvisTextDim,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No long-term memories learned yet.", color = JarvisTextSecondary, fontSize = 13.sp)
                Text("Tell Jarvis: \"Remember that my favorite app is Spotify\"", color = JarvisTextDim, fontSize = 11.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { memory ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memory.key.uppercase(),
                                color = ArcReactorCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = memory.value,
                                color = JarvisTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { onDelete(memory.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ArcReactorRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesVaultTab(
    notes: List<com.example.data.repository.UserNote>,
    onAddClick: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = ArcReactorCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_note_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = JarvisBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Vault Note", color = JarvisBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Vault notes are empty. Tell Jarvis \"Take a note...\"", color = JarvisTextDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = note.title, color = ArcReactorCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(onClick = { onDelete(note.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ArcReactorRed, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = note.content, color = JarvisTextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersTab(
    reminders: List<com.example.data.repository.UserReminder>,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No reminders set. Tell Jarvis \"Set reminder...\"", color = JarvisTextDim, fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders, key = { it.id }) { reminder ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggle(reminder.id, !reminder.isCompleted) }) {
                            Icon(
                                imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle",
                                tint = if (reminder.isCompleted) ArcReactorCyan else JarvisTextDim
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                color = if (reminder.isCompleted) JarvisTextDim else JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { onDelete(reminder.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ArcReactorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
