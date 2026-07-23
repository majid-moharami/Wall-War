package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WallWarTheme

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
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val isWallMode by viewModel.isWallMode.collectAsStateWithLifecycle()
    val isWallHorizontal by viewModel.isWallHorizontal.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
    val validHighlights by viewModel.validMoveHighlights.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
    val totalMatches by viewModel.totalMatchesCount.collectAsStateWithLifecycle()

    Crossfade(
        targetState = currentScreen,
        label = "screen_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.HOME -> {
                HomeScreen(
                    totalWins = totalWins,
                    totalMatches = totalMatches,
                    onStartGame = { mode, opponent, difficulty ->
                        viewModel.startNewGame(mode, opponent, difficulty)
                    },
                    onNavigate = { targetScreen ->
                        viewModel.navigateTo(targetScreen)
                    }
                )
            }
            AppScreen.GAME_BOARD -> {
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
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                )
            }
            AppScreen.RULES -> {
                RulesScreen(
                    onBack = {
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                )
            }
            AppScreen.HISTORY -> {
                HistoryScreen(
                    matchHistory = matchHistory,
                    totalWins = totalWins,
                    totalMatches = totalMatches,
                    onClearHistory = {
                        viewModel.clearHistory()
                    },
                    onBack = {
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    soundManager = viewModel.soundManager,
                    selectedTheme = boardTheme,
                    onSelectTheme = { theme ->
                        viewModel.setBoardTheme(theme)
                    },
                    onBack = {
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                )
            }
        }
    }
}
