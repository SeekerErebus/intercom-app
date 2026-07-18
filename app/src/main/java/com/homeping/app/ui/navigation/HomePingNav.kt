package com.homeping.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homeping.app.data.PreferencesRepository
import com.homeping.app.data.UserPreferences
import com.homeping.app.ui.MainScreen
import com.homeping.app.ui.settings.SettingsScreen
import com.homeping.app.ui.settings.SettingsViewModel
import com.homeping.app.ui.setup.SetupViewModel
import com.homeping.app.ui.setup.SetupWizardScreen
import kotlinx.coroutines.flow.first

object Routes {
    const val SETUP = "setup"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun HomePingNav() {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository.getInstance(context) }
    val prefs by repository.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences(
            deviceId = "",
            displayName = "",
            homePin = "",
            setupComplete = false,
        ),
    )

    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        repository.preferences.first()
        ready = true
    }

    if (!ready) {
        return
    }

    val navController = rememberNavController()
    val startDestination = if (prefs.setupComplete) Routes.MAIN else Routes.SETUP

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.SETUP) {
            val setupViewModel: SetupViewModel = viewModel(
                factory = SetupViewModel.Factory(repository),
            )
            SetupWizardScreen(
                viewModel = setupViewModel,
                onSetupFinished = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            val displayName = prefs.displayName
            MainScreen(
                peerName = prefs.pairedPeerName.takeIf { it.isNotBlank() } ?: "Other phone",
                statusText = if (prefs.setupComplete) {
                    "Ready on this phone as $displayName. Pairing comes next."
                } else {
                    "Not set up yet"
                },
                thisDeviceName = displayName,
                onPingClick = { /* PR6 */ },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository),
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
