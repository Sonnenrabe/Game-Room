package com.example.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DragDropState(
    val lazyListState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val haptic: HapticFeedback,
    private val onReorder: (List<Game>) -> Unit
) {
    var draggedGameId by mutableStateOf<Long?>(null)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    var displayedGames by mutableStateOf<List<Game>>(emptyList())
        private set

    val isDragging: Boolean
        get() = draggedGameId != null

    fun syncItems(items: List<Game>) {
        if (!isDragging) {
            displayedGames = items
        }
    }

    fun onDragStart(game: Game) {
        draggedGameId = game.id
        dragOffsetY = 0f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDrag(dragAmount: Float) {
        val currentId = draggedGameId ?: return
        dragOffsetY += dragAmount

        val currentIndex = displayedGames.indexOfFirst { it.id == currentId }
        if (currentIndex == -1) return

        val layoutInfo = lazyListState.layoutInfo
        val currentItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentId }

        if (currentItemInfo != null) {
            val itemSize = currentItemInfo.size.toFloat()
            val currentMid = currentItemInfo.offset + itemSize / 2f + dragOffsetY

            // Auto-scroll near boundaries
            val viewportStart = layoutInfo.viewportStartOffset.toFloat()
            val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
            val edgeThreshold = 120f

            if (currentMid < viewportStart + edgeThreshold) {
                val speed = ((viewportStart + edgeThreshold - currentMid) / edgeThreshold).coerceIn(0f, 1f) * 20f
                coroutineScope.launch {
                    lazyListState.scrollBy(-speed)
                }
            } else if (currentMid > viewportEnd - edgeThreshold) {
                val speed = ((currentMid - (viewportEnd - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f) * 20f
                coroutineScope.launch {
                    lazyListState.scrollBy(speed)
                }
            }

            // Move UP
            if (currentIndex > 0) {
                val prevGame = displayedGames[currentIndex - 1]
                val prevItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == prevGame.id }
                val prevMid = if (prevItemInfo != null) {
                    prevItemInfo.offset + prevItemInfo.size / 2f
                } else {
                    currentItemInfo.offset - itemSize / 2f
                }

                if (currentMid < prevMid) {
                    val updated = displayedGames.toMutableList()
                    val removed = updated.removeAt(currentIndex)
                    updated.add(currentIndex - 1, removed)
                    displayedGames = updated

                    val shift = if (prevItemInfo != null) {
                        (currentItemInfo.offset - prevItemInfo.offset).toFloat()
                    } else {
                        itemSize
                    }
                    dragOffsetY += shift
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }

            // Move DOWN
            if (currentIndex < displayedGames.size - 1) {
                val nextGame = displayedGames[currentIndex + 1]
                val nextItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == nextGame.id }
                val nextMid = if (nextItemInfo != null) {
                    nextItemInfo.offset + nextItemInfo.size / 2f
                } else {
                    currentItemInfo.offset + itemSize * 1.5f
                }

                if (currentMid > nextMid) {
                    val updated = displayedGames.toMutableList()
                    val removed = updated.removeAt(currentIndex)
                    updated.add(currentIndex + 1, removed)
                    displayedGames = updated

                    val shift = if (nextItemInfo != null) {
                        (nextItemInfo.offset - currentItemInfo.offset).toFloat()
                    } else {
                        itemSize
                    }
                    dragOffsetY -= shift
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        } else {
            // Fallback estimation when scrolling off-screen
            val estHeight = 220f
            if (currentIndex > 0 && dragOffsetY < -estHeight / 2f) {
                val updated = displayedGames.toMutableList()
                val removed = updated.removeAt(currentIndex)
                updated.add(currentIndex - 1, removed)
                displayedGames = updated
                dragOffsetY += estHeight
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (currentIndex < displayedGames.size - 1 && dragOffsetY > estHeight / 2f) {
                val updated = displayedGames.toMutableList()
                val removed = updated.removeAt(currentIndex)
                updated.add(currentIndex + 1, removed)
                displayedGames = updated
                dragOffsetY -= estHeight
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    fun onDragEnd() {
        val currentId = draggedGameId ?: return
        draggedGameId = null
        dragOffsetY = 0f
        onReorder(displayedGames)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun onDragCancel() {
        onDragEnd()
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    items: List<Game>,
    onReorder: (List<Game>) -> Unit
): DragDropState {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val state = remember(lazyListState) {
        DragDropState(
            lazyListState = lazyListState,
            coroutineScope = coroutineScope,
            haptic = haptic,
            onReorder = onReorder
        )
    }

    LaunchedEffect(items) {
        state.syncItems(items)
    }

    return state
}
