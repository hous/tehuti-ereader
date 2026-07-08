package com.tehuti.reader.reader.ai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSummaryDialog(
    excerpt: String,
    onDismiss: () -> Unit,
    viewModel: BookSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(excerpt) {
        viewModel.summarize(excerpt)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            when (val state = uiState) {
                SummaryUiState.Loading -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Summarizing what you've read…", modifier = Modifier.padding(top = 12.dp))
                }

                is SummaryUiState.Downloading -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(progress = { state.progress ?: 0f })
                    Text(
                        "Setting up on-device AI (first use)…",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                is SummaryUiState.Success -> Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyLarge,
                )

                SummaryUiState.NotEnoughContent -> Text(
                    text = "Keep reading a little more before summarizing.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                SummaryUiState.Unavailable -> Text(
                    text = "On-device AI summaries aren't supported on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                SummaryUiState.Error -> Text(
                    text = "Couldn't generate a summary right now.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
