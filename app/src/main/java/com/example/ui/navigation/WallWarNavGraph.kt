package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.AppScreen
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.game.GameViewModel
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.ranking.RankingScreen
import com.example.ui.screens.ranking.RankingViewModel
import com.example.ui.screens.settings.SettingsViewModel

@Composable
fun WallWarNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
            val totalMatches by viewModel.totalMatches.collectAsStateWithLifecycle()

            HomeScreen(
                totalWins = totalWins,
                totalMatches = totalMatches,
                onStartGame = { mode, opponent, difficulty ->
                    navController.navigate(
                        GameBoardRoute(
                            mode = mode.name,
                            opponent = opponent.name,
                            difficulty = difficulty.name
                        )
                    )
                },
                onNavigate = { targetScreen ->
                    when (targetScreen) {
                        AppScreen.GAME_BOARD -> navController.navigate(GameBoardRoute())
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

        composable<RankingRoute> {
            val viewModel: RankingViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

            RankingScreen(
                userProfile = userProfile,
                leaderboard = leaderboard
            )
        }

        composable<ProfileRoute> {
            val viewModel: ProfileViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val signInStatus by viewModel.signInStatus.collectAsStateWithLifecycle()

            ProfileScreen(
                userProfile = userProfile,
                signInStatus = signInStatus,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onClearSignInStatus = viewModel::clearSignInStatus,
                onSignOut = viewModel::signOut,
                onNavigateToHistory = { navController.navigate(HistoryRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<GameBoardRoute> {
            val viewModel: GameViewModel = hiltViewModel()
            val gameState by viewModel.gameState.collectAsStateWithLifecycle()
            val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
            val isWallMode by viewModel.isWallMode.collectAsStateWithLifecycle()
            val isWallHorizontal by viewModel.isWallHorizontal.collectAsStateWithLifecycle()
            val validHighlights by viewModel.validMoveHighlights.collectAsStateWithLifecycle()

            GameBoardScreen(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                soundManager = viewModel.soundManager,
                onCellClick = viewModel::selectCell,
                onPlaceWall = viewModel::placeWall,
                onSelectWallOrientation = viewModel::selectWallOrientation,
                onCancelWallMode = viewModel::cancelWallMode,
                onUndoMove = viewModel::undoMove,
                onRestart = viewModel::restartGame,
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
            val viewModel: HistoryViewModel = hiltViewModel()
            val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
            val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
            val totalMatches by viewModel.totalMatches.collectAsStateWithLifecycle()

            HistoryScreen(
                matchHistory = matchHistory,
                totalWins = totalWins,
                totalMatches = totalMatches,
                onClearHistory = viewModel::clearHistory,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val selectedTheme by viewModel.boardTheme.collectAsStateWithLifecycle()

            SettingsScreen(
                soundManager = viewModel.soundManager,
                selectedTheme = selectedTheme,
                onSelectTheme = viewModel::setBoardTheme,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }
    }
}
