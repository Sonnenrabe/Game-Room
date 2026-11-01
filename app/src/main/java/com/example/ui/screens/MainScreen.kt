package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Game
import com.example.ui.components.ActiveGameCard
import com.example.ui.components.AddGameDialog
import com.example.ui.components.ArchiveConfirmationDialog
import com.example.ui.components.ArchivedGameCard
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.EditNotesDialog
import com.example.ui.components.ImportConfirmationDialog
import com.example.ui.components.ManualAdjustTimeDialog
import com.example.ui.components.ReorderGameDialog
import com.example.ui.components.RestoreConfirmationDialog
import com.example.ui.components.TransferDataDialog
import com.example.ui.theme.GameDarkBackground
import com.example.ui.theme.GameDarkSurface
import com.example.ui.theme.GameDarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BackupSummary
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val runningGameId by viewModel.runningGameId.collectAsStateWithLifecycle()
    val elapsedSessionSeconds by viewModel.elapsedSessionSeconds.collectAsStateWithLifecycle()

    val activeGames by viewModel.filteredActiveGames.collectAsStateWithLifecycle()
    val archivedGames by viewModel.filteredArchivedGames.collectAsStateWithLifecycle()

    val allActiveGames by viewModel.allActiveGames.collectAsStateWithLifecycle()
    val allArchivedGames by viewModel.allArchivedGames.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Dialog state holders
    var gameToAdjust by remember { mutableStateOf<Game?>(null) }
    var gameToArchive by remember { mutableStateOf<Game?>(null) }
    var gameToRestore by remember { mutableStateOf<Game?>(null) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var gameToEditNotes by remember { mutableStateOf<Game?>(null) }
    var gameToReorder by remember { mutableStateOf<Game?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var exportJsonString by remember { mutableStateOf("") }
    var importSummaryToConfirm by remember { mutableStateOf<BackupSummary?>(null) }

    // Launcher for POST_NOTIFICATIONS permission on Android 13+
    var pendingGameToPlay by remember { mutableStateOf<Game?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingGameToPlay?.let {
            viewModel.playGame(it)
            pendingGameToPlay = null
        }
    }

    fun startPlayingWithNotificationCheck(game: Game) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                pendingGameToPlay = game
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.playGame(game)
    }

    // ActivityResultLauncher for Exporting JSON file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(exportJsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup successfully saved to file!", Toast.LENGTH_LONG).show()
                showTransferDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ActivityResultLauncher for Importing JSON file
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonContent = stringBuilder.toString()
                val parseResult = viewModel.parseBackupSummary(jsonContent)
                parseResult.onSuccess { summary ->
                    showTransferDialog = false
                    importSummaryToConfirm = summary
                }.onFailure { err ->
                    Toast.makeText(context, "Invalid backup format: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.background(GameDarkBackground)) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PLAYTIME",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                        }
                    },
                    actions = {
                        // Transfer & Backup Icon
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    exportJsonString = viewModel.exportDataAsJson()
                                    showTransferDialog = true
                                }
                            },
                            modifier = Modifier.testTag("transfer_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImportExport,
                                contentDescription = "Transfer and Backup Data",
                                tint = NeonCyan
                            )
                        }

                        // Search Toggle
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showSearchBar) "Close Search" else "Search Games",
                                tint = if (showSearchBar) NeonCyan else TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = GameDarkBackground
                    )
                )

                // Search Bar Expandable
                AnimatedVisibility(visible = showSearchBar) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by title or notes...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GameDarkSurfaceVariant,
                                focusedContainerColor = GameDarkSurface,
                                unfocusedContainerColor = GameDarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Tabs: Active vs Archived
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GameDarkBackground,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GameDarkSurfaceVariant)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        modifier = Modifier.testTag("tab_active_games"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ACTIVE GAMES",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = if (selectedTab == 0) NeonCyan else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedTab == 0) Color(0xFF003844) else Color.White.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "${allActiveGames.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedTab == 0) NeonCyan else TextTertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        modifier = Modifier.testTag("tab_archived_games"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ARCHIVED",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = if (selectedTab == 1) NeonCyan else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedTab == 1) Color(0xFF381578) else Color.White.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "${allArchivedGames.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedTab == 1) NeonPurple else TextTertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF002026),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = "TRACK GAME",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    modifier = Modifier.testTag("fab_add_game")
                )
            }
        },
        containerColor = GameDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // ACTIVE GAMES TAB
                if (activeGames.isEmpty()) {
                    EmptyStateView(
                        title = if (searchQuery.isNotEmpty()) "No matching games" else "No active games yet",
                        subtitle = if (searchQuery.isNotEmpty()) "Try clearing your search query" else "Tap below to track playtime on your favorite games!",
                        buttonText = if (searchQuery.isNotEmpty()) "Clear Search" else "Track First Game",
                        onButtonClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.setSearchQuery("")
                            } else {
                                showAddDialog = true
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(activeGames, key = { _, game -> game.id }) { _, game ->
                            val isRunning = (game.id == runningGameId)
                            ActiveGameCard(
                                game = game,
                                isRunning = isRunning,
                                elapsedSessionSeconds = if (isRunning) elapsedSessionSeconds else 0L,
                                onTogglePlayPause = {
                                    if (isRunning) {
                                        viewModel.pauseGame()
                                    } else {
                                        startPlayingWithNotificationCheck(game)
                                    }
                                },
                                onOpenAdjustTime = { gameToAdjust = game },
                                onOpenEditNotes = { gameToEditNotes = game },
                                onOpenArchive = { gameToArchive = game },
                                onLongClick = { gameToReorder = game }
                            )
                        }
                    }
                }
            } else {
                // ARCHIVED GAMES TAB
                if (archivedGames.isEmpty()) {
                    EmptyStateView(
                        title = if (searchQuery.isNotEmpty()) "No matching archived games" else "Archive is empty",
                        subtitle = if (searchQuery.isNotEmpty()) "Try clearing your search query" else "When you complete a game, tap the Archive button on its card to store its finished playtime here.",
                        buttonText = if (searchQuery.isNotEmpty()) "Clear Search" else "View Active Games",
                        onButtonClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.setSearchQuery("")
                            } else {
                                viewModel.setSelectedTab(0)
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Banner for Archive
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GameDarkSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CONQUERED GAMES",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${archivedGames.size} Titles Completed",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = TextPrimary
                                        )
                                    }
                                    val totalSeconds = archivedGames.sumOf { it.totalPlaytimeSeconds }
                                    val totalHours = totalSeconds / 3600
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonPurple.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${totalHours}h Logged",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = NeonPurple,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(archivedGames, key = { _, game -> game.id }) { _, game ->
                            ArchivedGameCard(
                                game = game,
                                onRestore = { gameToRestore = game },
                                onDelete = { gameToDelete = game },
                                onOpenEditNotes = { gameToEditNotes = game },
                                onLongClick = { gameToReorder = game }
                            )
                        }
                    }
                }
            }
        }
    }

    // Reorder Dialog (Triggered on long-pressing any game)
    gameToReorder?.let { game ->
        val currentList = if (game.isArchived) allArchivedGames else allActiveGames
        val currentIndex = currentList.indexOfFirst { it.id == game.id }
        if (currentIndex != -1) {
            ReorderGameDialog(
                game = game,
                currentIndex = currentIndex,
                totalGames = currentList.size,
                onMoveToTop = {
                    viewModel.moveGameToTop(game)
                    gameToReorder = null
                    Toast.makeText(context, "\"${game.title}\" moved to top!", Toast.LENGTH_SHORT).show()
                },
                onMoveUp = {
                    viewModel.moveGameUp(game)
                },
                onMoveDown = {
                    viewModel.moveGameDown(game)
                },
                onMoveToBottom = {
                    viewModel.moveGameToBottom(game)
                },
                onDismiss = { gameToReorder = null }
            )
        } else {
            gameToReorder = null
        }
    }

    // Dialogs
    gameToAdjust?.let { game ->
        ManualAdjustTimeDialog(
            game = game,
            onConfirm = { newSeconds ->
                viewModel.adjustPlaytime(game.id, newSeconds)
                gameToAdjust = null
            },
            onDismiss = { gameToAdjust = null }
        )
    }

    gameToEditNotes?.let { game ->
        EditNotesDialog(
            game = game,
            onSaveNotes = { newNotes ->
                viewModel.updateGameNotes(game.id, newNotes)
                gameToEditNotes = null
                Toast.makeText(context, "Notes updated!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { gameToEditNotes = null }
        )
    }

    gameToArchive?.let { game ->
        ArchiveConfirmationDialog(
            game = game,
            onConfirm = {
                viewModel.archiveGame(game.id)
                gameToArchive = null
            },
            onDismiss = { gameToArchive = null }
        )
    }

    gameToRestore?.let { game ->
        RestoreConfirmationDialog(
            game = game,
            onConfirm = {
                viewModel.restoreGame(game.id)
                gameToRestore = null
            },
            onDismiss = { gameToRestore = null }
        )
    }

    gameToDelete?.let { game ->
        DeleteConfirmationDialog(
            game = game,
            onConfirm = {
                viewModel.deleteGame(game.id)
                gameToDelete = null
            },
            onDismiss = { gameToDelete = null }
        )
    }

    if (showAddDialog) {
        AddGameDialog(
            onAddGame = { title, h, m, s, color, notes ->
                viewModel.addNewGame(title, h, m, s, color, notes)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showTransferDialog) {
        TransferDataDialog(
            exportJsonString = exportJsonString,
            onTriggerSaveFile = {
                val fileName = "playtime_backup_${System.currentTimeMillis() / 1000}.json"
                saveFileLauncher.launch(fileName)
            },
            onTriggerOpenFile = {
                openFileLauncher.launch(arrayOf("application/json", "text/*"))
            },
            onInspectImportText = { text ->
                val result = viewModel.parseBackupSummary(text)
                result.onSuccess { summary ->
                    showTransferDialog = false
                    importSummaryToConfirm = summary
                }.onFailure { err ->
                    Toast.makeText(context, "Invalid backup format: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { showTransferDialog = false }
        )
    }

    importSummaryToConfirm?.let { summary ->
        ImportConfirmationDialog(
            summary = summary,
            onConfirm = { replaceExisting ->
                viewModel.importGames(summary.games, replaceExisting) { count ->
                    importSummaryToConfirm = null
                    Toast.makeText(context, "Successfully imported $count games!", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { importSummaryToConfirm = null }
        )
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = GameDarkSurfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003844),
                contentColor = NeonCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}
