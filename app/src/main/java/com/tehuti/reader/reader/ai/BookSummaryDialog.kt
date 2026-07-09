package com.tehuti.reader.reader.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tehuti.reader.ui.LocalBlueLightFilterLevel
import com.tehuti.reader.ui.blueLightFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSummaryDialog(
    quickExcerpt: String,
    fullExcerpt: String,
    onDismiss: () -> Unit,
    viewModel: BookSummaryViewModel = hiltViewModel(),
) {
    val quickRecapState by viewModel.quickRecapState.collectAsState()
    val fullRecapState by viewModel.fullRecapState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var fullRecapRequested by remember { mutableStateOf(false) }

    LaunchedEffect(quickExcerpt) {
        viewModel.loadQuickRecap(quickExcerpt)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The sheet is its own window, so the app-root blue light filter doesn't reach it.
        modifier = Modifier.blueLightFilter(LocalBlueLightFilterLevel.current),
    ) {
        // The sheet doesn't scroll its content by default, so a long recap (or the full recap
        // stacked below the quick one) could otherwise overflow past the bottom of the screen
        // with no way to reach it — this makes the whole body a finger-scrollable column.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Here's where you left off…",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            RecapContent(quickRecapState)

            if (!fullRecapRequested) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = {
                        fullRecapRequested = true
                        viewModel.loadFullRecap(fullExcerpt)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Full recap up to your bookmark")
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Full recap up to your bookmark",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                fullRecapState?.let { RecapContent(it) }
            }
        }
    }
}

@Composable
private fun RecapContent(state: RecapUiState) {
    when (state) {
        RecapUiState.Loading -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }

        is RecapUiState.Downloading -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(progress = { state.progress ?: 0f })
            Text(
                "Setting up on-device AI (first use)…",
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        is RecapUiState.Success -> Text(
            text = state.text,
            style = MaterialTheme.typography.bodyLarge,
        )

        RecapUiState.NotEnoughContent -> Text(
            text = "Keep reading a little more before summarizing.",
            style = MaterialTheme.typography.bodyLarge,
        )

        RecapUiState.Unavailable -> Text(
            text = "AI summaries aren't available right now.",
            style = MaterialTheme.typography.bodyLarge,
        )

        RecapUiState.Error -> Text(
            text = "Couldn't generate a summary right now.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
