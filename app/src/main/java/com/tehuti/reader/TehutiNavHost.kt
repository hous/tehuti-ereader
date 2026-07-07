package com.tehuti.reader

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tehuti.reader.library.LibraryScreen

object TehutiDestinations {
    const val LIBRARY = "library"
}

@Composable
fun TehutiNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TehutiDestinations.LIBRARY,
    ) {
        composable(TehutiDestinations.LIBRARY) {
            LibraryScreen()
        }
    }
}
