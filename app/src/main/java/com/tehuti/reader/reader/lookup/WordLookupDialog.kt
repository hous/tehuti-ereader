package com.tehuti.reader.reader.lookup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tehuti.reader.domain.model.LookupResult
import com.tehuti.reader.domain.model.LookupType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupDialog(
    type: LookupType,
    word: String,
    onDismiss: () -> Unit,
    viewModel: WordLookupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(type, word) {
        viewModel.lookup(type, word)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            when (val state = uiState) {
                is LookupUiState.Loading -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Looking up “$word”…", modifier = Modifier.padding(top = 12.dp))
                }

                is LookupUiState.NotFound -> Text(
                    text = "No results found for “$word”.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                is LookupUiState.Success -> when (val result = state.result) {
                    is LookupResult.Dictionary -> DictionaryContent(result)
                    is LookupResult.Wikipedia -> WikipediaContent(result)
                }
            }
        }
    }
}

@Composable
private fun DictionaryContent(result: LookupResult.Dictionary) {
    Column {
        Text(text = result.word, style = MaterialTheme.typography.headlineSmall)
        result.phonetic?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge)
        }
        result.meanings.forEach { meaning ->
            Text(
                text = meaning.partOfSpeech,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            meaning.definitions.forEach { definition ->
                Text(
                    text = "• $definition",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WikipediaContent(result: LookupResult.Wikipedia) {
    val context = LocalContext.current
    Column {
        result.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = result.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = result.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = result.extract,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        result.pageUrl?.let { url ->
            Text(
                text = "Read more",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
            )
        }
    }
}
