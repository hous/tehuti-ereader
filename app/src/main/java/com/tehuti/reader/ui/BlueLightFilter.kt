package com.tehuti.reader.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp

/**
 * The current blue light filter level (0 = off, 1 = maximal). Provided at the app root so
 * composables that host their own window (the modal bottom sheets) can apply the same filter —
 * separate windows aren't covered by the root overlay.
 */
val LocalBlueLightFilterLevel = compositionLocalOf { 0f }

// The zero-blue amber the screen is multiplied toward as the level rises. Multiplication only
// ever removes light, so the filter darkens in every theme — unlike an alpha-composited orange
// scrim, which adds its own luminance and visibly *brightens* dark-mode pixels.
private val FilterTint = Color(0xFFFF6A00)

// Fraction of remaining brightness multiplied away at maximal filtering, on top of what the
// tint itself absorbs. Linear in the level so dimming responds across the whole slider range,
// not just near the top.
private const val MAX_DIM = 0.7f

/**
 * Filters this node's content for night reading by multiplying every pixel by a single warm
 * factor: white (no-op) at level 0, sliding toward [FilterTint] scaled by the dim fraction at
 * level 1. Blue reaches zero at maximal level, and overall brightness drops to ~30% of the
 * pixel's own value — far below the panel's minimum brightness on an already-dark theme.
 *
 * Modulate (pure component-wise multiply) rather than Multiply, so transparent areas of the
 * node (e.g. a sheet's rounded corners) stay transparent instead of being painted amber.
 */
fun Modifier.blueLightFilter(level: Float): Modifier = this
    .graphicsLayer {
        // The blend must composite against this node's own content only; without an offscreen
        // layer it would sample whatever happens to already be in the buffer behind the node.
        compositingStrategy =
            if (level > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithContent {
        drawContent()
        if (level > 0f) {
            val warm = lerp(Color.White, FilterTint, level)
            val brightness = 1f - MAX_DIM * level
            drawRect(
                color = Color(warm.red * brightness, warm.green * brightness, warm.blue * brightness),
                blendMode = BlendMode.Modulate,
            )
        }
    }
