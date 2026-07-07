package com.tehuti.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

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
            Text("Text size: ${settings.fontSizePercent}%", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.fontSizePercent.toFloat(),
                onValueChange = { viewModel.setFontSizePercent(it.toInt()) },
                valueRange = 50f..250f,
                steps = 19,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            Text("Font", style = MaterialTheme.typography.bodyLarge)
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
