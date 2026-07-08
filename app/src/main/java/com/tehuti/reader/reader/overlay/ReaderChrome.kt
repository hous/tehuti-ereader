package com.tehuti.reader.reader.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ReaderChrome(
    progression: Float,
    canReturnToPosition: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSeek: (Float) -> Unit,
    onReturnToPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onSeek((offset.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedProgression)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
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
