package com.goldenv2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.goldenv2.core.ui.theme.GoldenV2Theme
import com.goldenv2.feature.home.HomeScreen
import com.goldenv2.feature.home.HomeViewModel
import com.goldenv2.feature.logs.LogsScreen
import com.goldenv2.feature.logs.LogsViewModel
import com.goldenv2.feature.routing.RoutingScreen
import com.goldenv2.feature.routing.RoutingViewModel
import com.goldenv2.feature.servers.ServersScreen
import com.goldenv2.feature.servers.ServersViewModel
import com.goldenv2.feature.settings.SettingsScreen
import com.goldenv2.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val serversViewModel: ServersViewModel by viewModels()
    private val routingViewModel: RoutingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val logsViewModel: LogsViewModel by viewModels()

    private var vpnDialogPending = false

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            vpnDialogPending = false
            if (result.resultCode == RESULT_OK) {
                homeViewModel.onVpnPermissionGranted()
            } else {
                homeViewModel.onVpnPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectVpnPermissionRequests()
        setContent {
            GoldenV2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToRouting = { navController.navigate("routing") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLogs = { navController.navigate("logs") }
                            )
                        }
                        composable("servers") {
                            ServersScreen(
                                viewModel = serversViewModel,
                                onNavigateToHome = { navController.navigate("home") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLogs = { navController.navigate("logs") }
                            )
                        }
                        composable("routing") {
                            RoutingScreen(
                                viewModel = routingViewModel,
                                onNavigateToHome = { navController.navigate("home") },
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLogs = { navController.navigate("logs") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToHome = { navController.navigate("home") },
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToRouting = { navController.navigate("routing") },
                                onNavigateToLogs = { navController.navigate("logs") }
                            )
                        }
                        composable("logs") {
                            LogsScreen(
                                viewModel = logsViewModel,
                                onNavigateToHome = { navController.navigate("home") },
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToRouting = { navController.navigate("routing") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun collectVpnPermissionRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // MainActivity is the only collector: it owns the result callback
                // for this dialog. Collecting it in the composable as well would
                // race with that and open two system dialogs.
                homeViewModel.vpnPermissionRequest.collect { intent ->
                    if (vpnDialogPending) return@collect
                    vpnDialogPending = true
                    vpnPermissionLauncher.launch(intent)
                }
            }
        }
    }
}