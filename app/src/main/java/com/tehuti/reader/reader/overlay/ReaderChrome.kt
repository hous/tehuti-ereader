package com.tehuti.reader.reader.overlay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val COLLAPSED_TRACK_HEIGHT = 3.dp
private val EXPANDED_TRACK_HEIGHT = 6.dp
private val COLLAPSED_TOUCH_HEIGHT = 24.dp
private val EXPANDED_TOUCH_HEIGHT = 40.dp
private val COLLAPSED_BOOKMARK_HEIGHT = 10.dp
private val EXPANDED_BOOKMARK_HEIGHT = 20.dp
private val BOOKMARK_WIDTH = 3.dp
private const val EXPANDED_IDLE_TIMEOUT_MS = 3000L

@Composable
fun ReaderChrome(
    progression: Float,
    bookmarkProgression: Float?,
    canReturnToPosition: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSeek: (Float) -> Unit,
    onReturnToPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The progress bar starts collapsed and thin; the first tap only expands it into a bigger,
    // easier-to-aim target rather than seeking immediately, so a stray tap can't skip pages.
    var expanded by remember { mutableStateOf(false) }
    var lastInteractionAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(expanded, lastInteractionAt) {
        if (expanded) {
            delay(EXPANDED_IDLE_TIMEOUT_MS)
            expanded = false
        }
    }

    Box(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            if (canReturnToPosition) {
                TextButton(onClick = onReturnToPosition) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.height(16.dp),
                    )
                    Text(
                        text = "Return to where you left off",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val clampedProgression = progression.coerceIn(0f, 1f)
                val trackHeight by animateDpAsState(
                    if (expanded) EXPANDED_TRACK_HEIGHT else COLLAPSED_TRACK_HEIGHT,
                    label = "trackHeight",
                )
                val touchHeight by animateDpAsState(
                    if (expanded) EXPANDED_TOUCH_HEIGHT else COLLAPSED_TOUCH_HEIGHT,
                    label = "touchHeight",
                )
                val bookmarkHeight by animateDpAsState(
                    if (expanded) EXPANDED_BOOKMARK_HEIGHT else COLLAPSED_BOOKMARK_HEIGHT,
                    label = "bookmarkHeight",
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(touchHeight)
                        .pointerInput(expanded) {
                            detectTapGestures { offset ->
                                lastInteractionAt = System.currentTimeMillis()
                                if (!expanded) {
                                    expanded = true
                                } else {
                                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                        }
                        .pointerInput(expanded) {
                            if (expanded) {
                                detectDragGestures { change, _ ->
                                    lastInteractionAt = System.currentTimeMillis()
                                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedProgression)
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    if (bookmarkProgression != null) {
                        Box(
                            modifier = Modifier
                                .offset(x = maxWidth * bookmarkProgression.coerceIn(0f, 1f) - (BOOKMARK_WIDTH / 2))
                                .width(BOOKMARK_WIDTH)
                                .height(bookmarkHeight)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }
                Text(
                    text = "${(clampedProgression * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
