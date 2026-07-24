package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.navigation.GameBoardRoute
import com.example.ui.navigation.HistoryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.RulesRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WallWarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallWarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WallWarApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WallWarApp(
    viewModel: MainViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val isWallMode by viewModel.isWallMode.collectAsStateWithLifecycle()
    val isWallHorizontal by viewModel.isWallHorizontal.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
    val validHighlights by viewModel.validMoveHighlights.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
    val totalMatches by viewModel.totalMatchesCount.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier.fillMaxSize()
    ) {
        composable<HomeRoute> {
            HomeScreen(
                totalWins = totalWins,
                totalMatches = totalMatches,
                onStartGame = { mode, opponent, difficulty ->
                    viewModel.startNewGame(mode, opponent, difficulty)
                    navController.navigate(GameBoardRoute)
                },
                onNavigate = { targetScreen ->
                    when (targetScreen) {
                        AppScreen.GAME_BOARD -> navController.navigate(GameBoardRoute)
                        AppScreen.RULES -> navController.navigate(RulesRoute)
                        AppScreen.HISTORY -> navController.navigate(HistoryRoute)
                        AppScreen.SETTINGS -> navController.navigate(SettingsRoute)
                        AppScreen.HOME -> navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<GameBoardRoute> {
            GameBoardScreen(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                soundManager = viewModel.soundManager,
                onCellClick = { r, c ->
                    viewModel.selectCell(r, c)
                },
                onPlaceWall = { r, c, isHorizontal ->
                    viewModel.placeWall(r, c, isHorizontal)
                },
                onSelectWallOrientation = { isHorizontal ->
                    viewModel.selectWallOrientation(isHorizontal)
                },
                onUndoMove = {
                    viewModel.undoMove()
                },
                onRestart = {
                    viewModel.restartGame()
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<RulesRoute> {
            RulesScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<HistoryRoute> {
            HistoryScreen(
                matchHistory = matchHistory,
                totalWins = totalWins,
                totalMatches = totalMatches,
                onClearHistory = {
                    viewModel.clearHistory()
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                soundManager = viewModel.soundManager,
                selectedTheme = boardTheme,
                onSelectTheme = { theme ->
                    viewModel.setBoardTheme(theme)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }
    }
}
