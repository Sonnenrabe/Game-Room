package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Game
import com.example.ui.theme.GameAccentColors
import com.example.ui.theme.GameDarkSurface
import com.example.ui.theme.GameDarkSurfaceHighlight
import com.example.ui.theme.GameDarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BackupSummary

/**
 * Dialog to manually alter playtime with quick adjustment chips,
 * manual H/M/S inputs, live preview, and explicit confirmation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAdjustTimeDialog(
    game: Game,
    onConfirm: (newTotalSeconds: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var targetSeconds by remember { mutableLongStateOf(game.totalPlaytimeSeconds) }
    var hoursInput by remember { mutableStateOf((targetSeconds / 3600).toString()) }
    var minutesInput by remember { mutableStateOf(((targetSeconds % 3600) / 60).toString()) }
    var secondsInput by remember { mutableStateOf((targetSeconds % 60).toString()) }

    fun updateFromInputs(hStr: String, mStr: String, sStr: String) {
        val h = hStr.toLongOrNull() ?: 0L
        val m = mStr.toLongOrNull() ?: 0L
        val s = sStr.toLongOrNull() ?: 0L
        targetSeconds = (h * 3600L + m * 60L + s).coerceAtLeast(0L)
    }

    fun applyDelta(secondsDelta: Long) {
        val newSec = (targetSeconds + secondsDelta).coerceAtLeast(0L)
        targetSeconds = newSec
        hoursInput = (newSec / 3600).toString()
        minutesInput = ((newSec % 3600) / 60).toString()
        secondsInput = (newSec % 60).toString()
    }

    val diffSeconds = targetSeconds - game.totalPlaytimeSeconds
    val diffFormatted = when {
        diffSeconds > 0 -> {
            val dh = diffSeconds / 3600
            val dm = (diffSeconds % 3600) / 60
            val ds = diffSeconds % 60
            "+ ${dh}h ${dm}m ${ds}s"
        }
        diffSeconds < 0 -> {
            val posDiff = -diffSeconds
            val dh = posDiff / 3600
            val dm = (posDiff % 3600) / 60
            val ds = posDiff % 60
            "- ${dh}h ${dm}m ${ds}s"
        }
        else -> "No change"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Adjust Playtime",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(game.colorHex)
                )
                Text(
                    text = "Current: ${game.formattedPlaytime()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New Preview Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GameDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NEW PLAYTIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                        val h = targetSeconds / 3600
                        val m = (targetSeconds % 3600) / 60
                        val s = targetSeconds % 60
                        val formattedNew = String.format("%02d:%02d:%02d", h, m, s)
                        Text(
                            text = formattedNew,
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = diffFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diffSeconds >= 0) NeonGreen else NeonRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick Adjustments",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "+15m" to 15 * 60L,
                        "+30m" to 30 * 60L,
                        "+1h" to 3600L,
                        "+5h" to 5 * 3600L,
                        "-15m" to -15 * 60L,
                        "-30m" to -30 * 60L,
                        "-1h" to -3600L
                    ).forEach { (label, delta) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (delta > 0) Color(0xFF003844) else Color(0xFF38151E),
                            modifier = Modifier
                                .clickable { applyDelta(delta) }
                                .padding(1.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (delta > 0) NeonCyan else NeonRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Exact Duration",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = {
                            hoursInput = it
                            updateFromInputs(it, minutesInput, secondsInput)
                        },
                        label = { Text("Hours", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("hours_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextTertiary
                        )
                    )
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = {
                            minutesInput = it
                            updateFromInputs(hoursInput, it, secondsInput)
                        },
                        label = { Text("Mins", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("mins_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextTertiary
                        )
                    )
                    OutlinedTextField(
                        value = secondsInput,
                        onValueChange = {
                            secondsInput = it
                            updateFromInputs(hoursInput, minutesInput, it)
                        },
                        label = { Text("Secs", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("secs_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextTertiary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Confirming will permanently update the stored playtime for this game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetSeconds) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                modifier = Modifier.testTag("confirm_adjust_time_button")
            ) {
                Text("Confirm Change", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Dialog to view and edit freeform notes for any game.
 */
@Composable
fun EditNotesDialog(
    game: Game,
    onSaveNotes: (notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var notesText by remember { mutableStateOf(game.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Game Notes",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(game.colorHex)
                )
                Text(
                    text = "Playtime: ${game.formattedPlaytime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Add thoughts, current quest, completion status, difficulty notes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .testTag("edit_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextTertiary,
                        focusedContainerColor = GameDarkSurfaceVariant,
                        unfocusedContainerColor = GameDarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveNotes(notesText) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                modifier = Modifier.testTag("save_notes_button")
            ) {
                Text("Save Notes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Confirmation dialog before moving a game to the Archived tab.
 */
@Composable
fun ArchiveConfirmationDialog(
    game: Game,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Archive Game?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Move \"${game.title}\" to your archive?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GameDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL PLAYTIME LOGGED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text(
                            text = game.formattedPlaytime(),
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = NeonCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can view its stats and restore it back to active tracking at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                modifier = Modifier.testTag("confirm_archive_button")
            ) {
                Text("Archive Game", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Confirmation dialog before restoring an archived game back to the Active tab.
 */
@Composable
fun RestoreConfirmationDialog(
    game: Game,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Restore Game?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Restore \"${game.title}\" to active tracking?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The game will return to your Active tab with its current playtime (${game.formattedPlaytime()}) so you can resume playing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00391A)),
                modifier = Modifier.testTag("confirm_restore_button")
            ) {
                Text("Restore to Active", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Confirmation dialog before permanently deleting an archived game.
 */
@Composable
fun DeleteConfirmationDialog(
    game: Game,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Permanently Delete?",
                style = MaterialTheme.typography.titleLarge,
                color = NeonRed,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Are you sure you want to delete \"${game.title}\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will permanently remove the game and erase its ${game.formattedPlaytime()} tracked playtime. This action CANNOT be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete Permanently", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Dialog to add a new game with title, initial playtime, accent color, and freeform notes.
 */
@Composable
fun AddGameDialog(
    onAddGame: (
        title: String,
        hours: Long,
        minutes: Long,
        seconds: Long,
        colorHex: Long,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isTitleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Track New Game",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Game Title *") },
                    isError = isTitleError,
                    supportingText = if (isTitleError) {
                        { Text("Title cannot be empty") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_game_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextTertiary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Starting Playtime (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Hours", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Minutes", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Freeform Notes (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Notes, goals, current status, playthrough thoughts...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 150.dp)
                        .testTag("add_game_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextTertiary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameAccentColors.forEachIndexed { index, hex ->
                        val color = Color(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorIndex == index) 2.5.dp else 0.dp,
                                    color = if (selectedColorIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                    } else {
                        onAddGame(
                            title,
                            hours.toLongOrNull() ?: 0L,
                            minutes.toLongOrNull() ?: 0L,
                            0L,
                            GameAccentColors[selectedColorIndex],
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                modifier = Modifier.testTag("submit_add_game_button")
            ) {
                Text("Add Game", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Transfer / Backup & Restore Dialog:
 * Export data to JSON (file, clipboard, or share), or Import data from JSON file / text.
 */
@Composable
fun TransferDataDialog(
    exportJsonString: String,
    onTriggerSaveFile: () -> Unit,
    onTriggerOpenFile: () -> Unit,
    onInspectImportText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var pasteInputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transfer & Backup Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GameDarkSurfaceVariant,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("EXPORT", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("IMPORT", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // EXPORT TAB
                    Text(
                        text = "Save your games and playtime records to a JSON backup file to move to another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTriggerSaveFile,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save File", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, exportJsonString)
                                    type = "application/json"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share PlayTime Backup")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003844), contentColor = NeonCyan),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("PlayTime Backup", exportJsonString)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON to Clipboard", color = TextPrimary)
                    }
                } else {
                    // IMPORT TAB
                    Text(
                        text = "Select a backup file (.json) from your device or paste the backup content below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onTriggerOpenFile,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF002026)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Backup File (.json)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Or paste backup JSON:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = pasteInputText,
                        onValueChange = { pasteInputText = it },
                        placeholder = { Text("Paste JSON here...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextTertiary
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)
                                val text = item?.text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    pasteInputText = text
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (pasteInputText.isNotBlank()) {
                                onInspectImportText(pasteInputText)
                            } else {
                                Toast.makeText(context, "Please paste or select a JSON backup first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003844), contentColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Inspect & Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}

/**
 * Confirmation dialog before importing backup games, giving the user the choice
 * to Merge with existing data or Replace everything.
 */
@Composable
fun ImportConfirmationDialog(
    summary: BackupSummary,
    onConfirm: (replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Confirm Data Import",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GameDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BACKUP SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${summary.totalCount} Games Found",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = "${summary.activeCount} Active • ${summary.archivedCount} Archived • ${summary.totalPlaytimeFormatted} playtime",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Import Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Merge option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExisting = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !replaceExisting,
                        onClick = { replaceExisting = false },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Merge with current games", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Keeps your current list and adds backup games", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }

                // Replace option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExisting = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = replaceExisting,
                        onClick = { replaceExisting = true },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Replace all current games", style = MaterialTheme.typography.bodyMedium, color = if (replaceExisting) NeonRed else TextPrimary)
                        Text("Clears current list and restores exact backup", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceExisting) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (replaceExisting) NeonRed else NeonCyan,
                    contentColor = if (replaceExisting) Color.White else Color(0xFF002026)
                ),
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(if (replaceExisting) "Replace & Import" else "Merge & Import", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}
