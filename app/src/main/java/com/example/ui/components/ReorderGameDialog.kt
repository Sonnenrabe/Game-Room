package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Game
import com.example.ui.theme.GameDarkSurface
import com.example.ui.theme.GameDarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ReorderGameDialog(
    game: Game,
    currentIndex: Int,
    totalGames: Int,
    onMoveToTop: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToBottom: () -> Unit,
    onDismiss: () -> Unit
) {
    val isTop = currentIndex == 0
    val isBottom = currentIndex == totalGames - 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FormatLineSpacing,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reorder Game",
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
                // Game Header & Current Position Badge
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(game.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GameDarkSurfaceVariant
                    ) {
                        Text(
                            text = "#${currentIndex + 1} of $totalGames",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Prominent Action: MOVE TO TOP (Put on Top)
                Button(
                    onClick = onMoveToTop,
                    enabled = !isTop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF002026),
                        disabledContainerColor = GameDarkSurfaceVariant,
                        disabledContentColor = TextTertiary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("reorder_move_to_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignTop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTop) "ALREADY AT TOP" else "MOVE TO TOP",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Step Buttons: Move Up & Move Down
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMoveUp,
                        enabled = !isTop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF003844),
                            contentColor = NeonCyan,
                            disabledContainerColor = GameDarkSurfaceVariant,
                            disabledContentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("reorder_move_up_button")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move Up", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onMoveDown,
                        enabled = !isBottom,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF003844),
                            contentColor = NeonCyan,
                            disabledContainerColor = GameDarkSurfaceVariant,
                            disabledContentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("reorder_move_down_button")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move Down", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Move to Bottom
                OutlinedButton(
                    onClick = onMoveToBottom,
                    enabled = !isBottom,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("reorder_move_to_bottom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (!isBottom) TextSecondary else TextTertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move to Bottom",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isBottom) TextSecondary else TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tip: You can long-press any game card at any time to reorder it or put it on top.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Done", color = TextSecondary)
            }
        },
        containerColor = GameDarkSurface
    )
}
