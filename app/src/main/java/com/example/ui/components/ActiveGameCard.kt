package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Game
import com.example.ui.theme.GameDarkSurface
import com.example.ui.theme.GameDarkSurfaceHighlight
import com.example.ui.theme.GameDarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ActiveGameCard(
    game: Game,
    isRunning: Boolean,
    elapsedSessionSeconds: Long,
    onTogglePlayPause: () -> Unit,
    onOpenAdjustTime: () -> Unit,
    onOpenEditNotes: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayPlaytime = if (isRunning) {
        game.formattedPlaytime(elapsedSessionSeconds)
    } else {
        game.formattedPlaytime(0L)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isRunning) NeonGreen else Color(game.colorHex).copy(alpha = 0.35f),
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isRunning) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("game_card_${game.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) GameDarkSurfaceHighlight else GameDarkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title and Status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(game.colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NeonGreen.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                                    .alpha(pulseAlpha)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RUNNING",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // Freeform Notes Section
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GameDarkSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEditNotes() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (game.notes.isNotBlank()) NeonCyan else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (game.notes.isNotBlank()) game.notes else "Tap to add notes on this game...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (game.notes.isNotBlank()) TextSecondary else TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit notes",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Massive Digital Timer Display (HH:MM:SS)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A0E17),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRunning) NeonGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRunning) "SESSION IN PROGRESS" else "TOTAL PLAYTIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isRunning) NeonGreen else TextTertiary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = displayPlaytime,
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = if (isRunning) NeonGreen else NeonCyan,
                            modifier = Modifier.testTag("playtime_text_${game.id}")
                        )
                    }

                    if (isRunning) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "THIS SESSION",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                letterSpacing = 0.8.sp
                            )
                            val sessionH = elapsedSessionSeconds / 3600
                            val sessionM = (elapsedSessionSeconds % 3600) / 60
                            val sessionS = elapsedSessionSeconds % 60
                            val sessionFormatted = String.format("+%02d:%02d:%02d", sessionH, sessionM, sessionS)
                            Text(
                                text = sessionFormatted,
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Play/Pause, Adjust Time, Notes, Archive
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Play / Pause Button
                Button(
                    onClick = onTogglePlayPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) NeonAmber else NeonCyan,
                        contentColor = Color(0xFF0A0E17)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("play_pause_button_${game.id}")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause Timer" else "Start Timer",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "PAUSE" else "PLAY",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Adjust Time Button
                OutlinedIconButton(
                    onClick = onOpenAdjustTime,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("adjust_time_button_${game.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Adjust Playtime Manually",
                        tint = NeonCyan
                    )
                }

                // Archive Game Button
                OutlinedIconButton(
                    onClick = onOpenArchive,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("archive_game_button_${game.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Finish and Archive Game",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}
