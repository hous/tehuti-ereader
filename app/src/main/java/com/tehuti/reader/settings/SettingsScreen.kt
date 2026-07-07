package com.tehuti.reader.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme
import com.tehuti.reader.ui.theme.TehutiInkDark
import com.tehuti.reader.ui.theme.TehutiInkLight
import com.tehuti.reader.ui.theme.TehutiInkSepia
import com.tehuti.reader.ui.theme.TehutiPaperDark
import com.tehuti.reader.ui.theme.TehutiPaperLight
import com.tehuti.reader.ui.theme.TehutiPaperSepia

private const val PREVIEW_TEXT = "The universe is a work of art, signed by an unknown master."

/** Base EPUB body text size at 100%, matching the reader's default. */
private const val BASE_PREVIEW_FONT_SIZE_SP = 16f

/** 1.5x is the minimum readable line spacing for body text (WCAG 1.4.8). */
private const val PREVIEW_LINE_HEIGHT_RATIO = 1.5f

private fun AppFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    AppFontFamily.SERIF -> FontFamily.Serif
    AppFontFamily.SANS_SERIF -> FontFamily.SansSerif
    AppFontFamily.MONOSPACE -> FontFamily.Monospace
}

private fun AppTheme.previewColors(): Pair<Color, Color> = when (this) {
    AppTheme.LIGHT -> TehutiPaperLight to TehutiInkLight
    AppTheme.DARK -> TehutiPaperDark to TehutiInkDark
    AppTheme.SEPIA -> TehutiPaperSepia to TehutiInkSepia
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val booksFolderName by viewModel.booksFolderName.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            viewModel.onBooksFolderPicked(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(24.dp)) {
            Text("Books folder", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = booksFolderName ?: "Not selected",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { folderPickerLauncher.launch(null) }) {
                    Text("Change")
                }
            }

            var sliderPercent by remember(settings.fontSizePercent) {
                mutableFloatStateOf(settings.fontSizePercent.toFloat())
            }

            Text("Text size: ${sliderPercent.toInt()}%", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = sliderPercent,
                onValueChange = { sliderPercent = it },
                onValueChangeFinished = { viewModel.setFontSizePercent(sliderPercent.toInt()) },
                valueRange = 50f..250f,
                steps = 19,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            val (previewBackground, previewInk) = settings.theme.previewColors()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewBackground)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(20.dp),
            ) {
                val previewFontSize = (BASE_PREVIEW_FONT_SIZE_SP * sliderPercent / 100f).sp
                Text(
                    text = PREVIEW_TEXT,
                    color = previewInk,
                    fontFamily = settings.fontFamily.toComposeFontFamily(),
                    fontSize = previewFontSize,
                    lineHeight = previewFontSize * PREVIEW_LINE_HEIGHT_RATIO,
                )
            }

            Text("Font", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 24.dp))
            Row(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp).fillMaxWidth()) {
                AppFontFamily.entries.forEach { fontFamily ->
                    FilterChip(
                        selected = settings.fontFamily == fontFamily,
                        onClick = { viewModel.setFontFamily(fontFamily) },
                        label = { Text(fontFamily.displayName) },
                        modifier = Modifier.padding(end = 8.dp).wrapContentWidth(),
                    )
                }
            }

            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 8.dp).wrapContentWidth(),
                    )
                }
            }
        }
    }
}
