package com.wallwar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wallwar.ui.AppScreen
import com.wallwar.ui.screens.GameBoardScreen
import com.wallwar.ui.screens.HistoryScreen
import com.wallwar.ui.screens.HomeScreen
import com.wallwar.ui.screens.RulesScreen
import com.wallwar.ui.screens.SettingsScreen
import com.wallwar.ui.screens.game.GameViewModel
import com.wallwar.ui.screens.history.HistoryViewModel
import com.wallwar.ui.screens.home.HomeViewModel
import com.wallwar.ui.screens.profile.ProfileScreen
import com.wallwar.ui.screens.profile.ProfileViewModel
import com.wallwar.ui.screens.ranking.RankingScreen
import com.wallwar.ui.screens.ranking.RankingViewModel
import com.wallwar.ui.screens.settings.SettingsViewModel

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
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
            val totalMatches by viewModel.totalMatches.collectAsStateWithLifecycle()

            HomeScreen(
                userProfile = userProfile,
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
            val friends by viewModel.friends.collectAsStateWithLifecycle()

            ProfileScreen(
                userProfile = userProfile,
                signInStatus = signInStatus,
                friends = friends,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onClearSignInStatus = viewModel::clearSignInStatus,
                onSignOut = viewModel::signOut,
                onAddFriend = viewModel::addFriend,
                onRemoveFriend = viewModel::removeFriend,
                onChallengeFriend = { friendUsername ->
                    navController.navigate(GameBoardRoute(opponent = "ONLINE"))
                },
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
            val onlineMatchState by viewModel.onlineMatchState.collectAsStateWithLifecycle()
            val onlineOpponentName by viewModel.onlineOpponentName.collectAsStateWithLifecycle()
            val myPlayerIndex by viewModel.myPlayerIndex.collectAsStateWithLifecycle()
            val onlineErrorMessage by viewModel.onlineErrorMessage.collectAsStateWithLifecycle()

            GameBoardScreen(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                soundManager = viewModel.soundManager,
                opponentType = viewModel.opponentType,
                onlineMatchState = onlineMatchState,
                onlineOpponentName = onlineOpponentName,
                myPlayerIndex = myPlayerIndex,
                onlineErrorMessage = onlineErrorMessage,
                onRetryOnlineConnection = viewModel::startOnlineMatchmaking,
                onCancelOnlineMatchmaking = viewModel::cancelOnlineMatchmaking,
                onCellClick = viewModel::selectCell,
                onPlaceWall = viewModel::placeWall,
                onSelectWallOrientation = viewModel::selectWallOrientation,
                onCancelWallMode = viewModel::toggleWallMode,
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
            val nakamaConfig by viewModel.nakamaConfig.collectAsStateWithLifecycle()

            SettingsScreen(
                soundManager = viewModel.soundManager,
                selectedTheme = selectedTheme,
                nakamaConfig = nakamaConfig,
                onSelectTheme = viewModel::setBoardTheme,
                onUpdateNakamaConfig = viewModel::updateNakamaConfig,
                onTestConnection = viewModel::testNakamaConnection,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }
    }
}
