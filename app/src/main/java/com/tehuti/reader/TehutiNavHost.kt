package com.tehuti.reader

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tehuti.reader.library.LibraryScreen
import com.tehuti.reader.reader.ReaderScreen
import com.tehuti.reader.settings.SettingsScreen

object TehutiDestinations {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val SETTINGS = "settings"

    fun reader(bookId: String) = "reader/$bookId"
}

@Composable
fun TehutiNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TehutiDestinations.LIBRARY,
    ) {
        composable(TehutiDestinations.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(TehutiDestinations.reader(bookId)) },
                onSettingsClick = { navController.navigate(TehutiDestinations.SETTINGS) },
            )
        }
        composable(
            route = TehutiDestinations.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) {
            ReaderScreen(
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(TehutiDestinations.SETTINGS) },
            )
        }
        composable(TehutiDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
