package com.wallwar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallwar.data.AuthRepository
import com.wallwar.data.UserProfile
import com.wallwar.data.ad.AdManager
import com.wallwar.ui.components.AdOverlayDialog
import com.wallwar.ui.navigation.HomeRoute
import com.wallwar.ui.navigation.ProfileRoute
import com.wallwar.ui.navigation.RankingRoute
import com.wallwar.ui.navigation.WallWarNavGraph
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonMagenta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    val adManager: AdManager
) : ViewModel() {
    val userProfile: StateFlow<UserProfile> = authRepository.userProfile
    val isAdPlaying = adManager.isAdPlaying
    val activeNetwork = adManager.activeNetwork
    val currentAdType = adManager.currentAdType
    val adCountdown = adManager.adCountdown
    val rewardDescription = adManager.rewardDescription
}

sealed class BottomTab(
    val route: Any,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomTab(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Ranking : BottomTab(RankingRoute, "Ranking", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
    object Profile : BottomTab(ProfileRoute, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainContainerScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAdPlaying by viewModel.isAdPlaying.collectAsStateWithLifecycle()
    val activeNetwork by viewModel.activeNetwork.collectAsStateWithLifecycle()
    val currentAdType by viewModel.currentAdType.collectAsStateWithLifecycle()
    val adCountdown by viewModel.adCountdown.collectAsStateWithLifecycle()
    val rewardDescription by viewModel.rewardDescription.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(BottomTab.Home, BottomTab.Ranking, BottomTab.Profile)

    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        tabs.any { tab ->
            val simpleName = tab.route::class.simpleName ?: ""
            simpleName.isNotEmpty() && destination.route?.contains(simpleName) == true
        }
    } == true

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NeonDarkBg,
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        color = NeonDarkSurface,
                        tonalElevation = 8.dp
                    ) {
                        Column {
                            // Glowing Cyber Accent Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta, NeonCyan)))
                            )

                            NavigationBar(
                                containerColor = NeonDarkSurface,
                                contentColor = Color.White,
                                tonalElevation = 0.dp
                            ) {
                                tabs.forEach { tab ->
                                    val tabName = tab.route::class.simpleName ?: ""
                                    val isSelected = currentDestination?.hierarchy?.any {
                                        it.route?.contains(tabName) == true
                                    } == true

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonCyan,
                                            selectedTextColor = NeonCyan,
                                            indicatorColor = Color(0xFF003847),
                                            unselectedIconColor = Color(0xFFA0ACCC),
                                            unselectedTextColor = Color(0xFFA0ACCC)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            WallWarNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Global Dual Ad Overlay Dialog
        AdOverlayDialog(
            isAdPlaying = isAdPlaying,
            network = activeNetwork,
            adType = currentAdType,
            countdown = adCountdown,
            rewardDescription = rewardDescription
        )
    }
}
