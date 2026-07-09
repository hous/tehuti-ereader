package com.tehuti.reader

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.tehuti.reader.domain.model.ReaderSettings
import com.tehuti.reader.domain.repo.SettingsRepository
import com.tehuti.reader.ui.LocalBlueLightFilterLevel
import com.tehuti.reader.ui.blueLightFilter
import com.tehuti.reader.ui.theme.TehutiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val contentReady = mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !contentReady.value }

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = ReaderSettings())
            TehutiTheme(theme = settings.theme) {
                // The filter is drawn over the whole window here; bottom sheets are separate
                // windows and pick the level up via LocalBlueLightFilterLevel instead.
                CompositionLocalProvider(LocalBlueLightFilterLevel provides settings.blueLightFilter) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .blueLightFilter(settings.blueLightFilter),
                    ) {
                        TehutiNavHost()
                    }
                }
            }
            LaunchedEffect(Unit) {
                contentReady.value = true
            }
        }
    }
}
