@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.tehuti.reader.reader

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.hilt.navigation.compose.hiltViewModel
import com.tehuti.reader.reader.overlay.ReaderChrome
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as FragmentActivity

    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        is ReaderUiState.Loading -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is ReaderUiState.Error -> Box(Modifier.fillMaxSize()) {
            Text(
                text = "Couldn't open this book.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        is ReaderUiState.Ready -> ReaderContent(
            state = state,
            activity = activity,
            viewModel = viewModel,
            onBack = onBack,
            onSettings = onSettings,
        )
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState.Ready,
    activity: FragmentActivity,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val containerId = rememberSaveable { View.generateViewId() }
    var navigator by remember { mutableStateOf<OverflowableNavigator?>(null) }
    var chromeVisible by remember { mutableStateOf(false) }
    var progression by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (chromeVisible) chromeVisible = false else onBack()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
    )

    LaunchedEffect(state) {
        val fm = activity.supportFragmentManager
        fm.fragmentFactory = state.fragmentFactory
        var fragment = fm.findFragmentById(containerId)
        if (fragment == null) {
            fm.commitNow { add(containerId, state.fragmentClass, null) }
            fragment = fm.findFragmentById(containerId)
        }
        navigator = fragment as? OverflowableNavigator
    }

    DisposableEffect(Unit) {
        onDispose {
            val fm = activity.supportFragmentManager
            fm.findFragmentById(containerId)?.let { fragment ->
                fm.commitNow { remove(fragment) }
            }
        }
    }

    LaunchedEffect(navigator) {
        val nav = navigator ?: return@LaunchedEffect
        nav.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val width = nav.publicationView.width
                if (width <= 0) return false
                val xFraction = event.point.x / width
                when {
                    xFraction <= 0.25f -> nav.goBackward(true)
                    xFraction >= 0.75f -> nav.goForward(true)
                    else -> chromeVisible = !chromeVisible
                }
                return true
            }
        })
        nav.currentLocator.collect { locator ->
            progression = locator.locations.totalProgression?.toFloat() ?: 0f
            viewModel.onLocatorChanged(locator)
        }
    }

    if (chromeVisible) {
        ReaderChrome(
            progression = progression,
            onBack = onBack,
            onSettings = onSettings,
            onSeek = { fraction ->
                coroutineScope.launch {
                    val locator = viewModel.findLocatorForProgression(fraction)
                    if (locator != null) navigator?.go(locator, true)
                }
            },
        )
    }
}
