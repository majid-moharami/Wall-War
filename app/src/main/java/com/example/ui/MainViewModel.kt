package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.GameRepository
import com.example.data.MatchRecord
import com.example.engine.AiEngine
import com.example.engine.GameEngine
import com.example.model.AiDifficulty
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Move
import com.example.model.OpponentType
import com.example.model.Position
import com.example.model.Wall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AppScreen {
    HOME,
    GAME_BOARD,
    RULES,
    HISTORY,
    SETTINGS
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: GameRepository,
    val soundManager: SoundManager
) : ViewModel() {

    val matchHistory: StateFlow<List<MatchRecord>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWins: StateFlow<Int> = repository.playerWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMatchesCount: StateFlow<Int> = repository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gameState = MutableStateFlow(GameEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _isWallMode = MutableStateFlow(false)
    val isWallMode: StateFlow<Boolean> = _isWallMode.asStateFlow()

    private val _isWallHorizontal = MutableStateFlow(true)
    val isWallHorizontal: StateFlow<Boolean> = _isWallHorizontal.asStateFlow()

    private val _opponentType = MutableStateFlow(OpponentType.AI)
    val opponentType: StateFlow<OpponentType> = _opponentType.asStateFlow()

    private val _aiDifficulty = MutableStateFlow(AiDifficulty.NORMAL)
    val aiDifficulty: StateFlow<AiDifficulty> = _aiDifficulty.asStateFlow()

    private val _boardTheme = MutableStateFlow(BoardTheme.ELEGANT_DARK)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition.asStateFlow()

    private val _validMoveHighlights = MutableStateFlow<List<Position>>(emptyList())
    val validMoveHighlights: StateFlow<List<Position>> = _validMoveHighlights.asStateFlow()

    private var matchStartTime = System.currentTimeMillis()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun startNewGame(
        mode: GameMode = GameMode.DUEL,
        opponent: OpponentType = OpponentType.AI,
        difficulty: AiDifficulty = AiDifficulty.NORMAL
    ) {
        _opponentType.value = opponent
        _aiDifficulty.value = difficulty
        _isWallMode.value = false
        _selectedPosition.value = null

        val initialState = GameEngine.createInitialState(mode).copy(
            isAiMatch = opponent == OpponentType.AI,
            aiDifficulty = difficulty
        )
        _gameState.value = initialState
        _validMoveHighlights.value = GameEngine.pawnMoves(initialState, initialState.turn)
        matchStartTime = System.currentTimeMillis()

        _currentScreen.value = AppScreen.GAME_BOARD
    }

    fun toggleWallMode() {
        _isWallMode.value = !_isWallMode.value
        if (_isWallMode.value) {
            _selectedPosition.value = null
        }
        soundManager.vibrateShort()
    }

    fun toggleWallOrientation() {
        _isWallHorizontal.value = !_isWallHorizontal.value
        soundManager.vibrateShort()
    }

    fun selectWallOrientation(isHorizontal: Boolean) {
        if (_isWallMode.value && _isWallHorizontal.value == isHorizontal) {
            _isWallMode.value = false
        } else {
            _isWallMode.value = true
            _isWallHorizontal.value = isHorizontal
            _selectedPosition.value = null
        }
        soundManager.vibrateShort()
    }

    fun placeWall(r: Int, c: Int, isHorizontal: Boolean) {
        val state = _gameState.value
        if (state.isGameOver()) return
        if (state.isAiMatch && state.turn == 1) return

        val wall = Wall(r, c, isHorizontal, playerOwner = state.turn)
        if (GameEngine.canPlaceWall(state, state.turn, wall)) {
            applyUserMove(Move.WallPlacement(wall))
            _isWallMode.value = false
        } else {
            soundManager.playErrorSound()
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
    }

    fun selectCell(r: Int, c: Int) {
        val state = _gameState.value
        if (state.isGameOver()) return

        // If it's AI's turn in an AI match, ignore user inputs
        if (state.isAiMatch && state.turn == 1) return

        if (_isWallMode.value) {
            // Attempt wall placement
            val wall = Wall(r, c, _isWallHorizontal.value, playerOwner = state.turn)
            if (GameEngine.canPlaceWall(state, state.turn, wall)) {
                applyUserMove(Move.WallPlacement(wall))
                _isWallMode.value = false
            } else {
                soundManager.playErrorSound()
            }
        } else {
            // Pawn step placement
            val legalMoves = GameEngine.pawnMoves(state, state.turn)
            val clickedTarget = Position(r, c)
            if (legalMoves.contains(clickedTarget)) {
                applyUserMove(Move.PawnStep(clickedTarget))
            } else {
                val myPawn = state.pawns[state.turn]
                if (r == myPawn.r && c == myPawn.c) {
                    _validMoveHighlights.value = legalMoves
                } else {
                    soundManager.playErrorSound()
                }
            }
        }
    }

    private fun applyUserMove(move: Move) {
        val currentState = _gameState.value
        val nextState = GameEngine.applyMove(currentState, move) ?: return

        val isWall = move is Move.WallPlacement
        soundManager.playMoveSound(isMine = true, isWall = isWall)
        soundManager.vibrateShort()

        _gameState.value = nextState
        _validMoveHighlights.value = GameEngine.pawnMoves(nextState, nextState.turn)

        checkGameEndAndTriggerAiIfNeeded(nextState)
    }

    private fun checkGameEndAndTriggerAiIfNeeded(state: GameState) {
        if (state.winner != null) {
            soundManager.playVictoryFanfare()
            soundManager.vibrateSuccess()
            saveMatchToHistory(state)
            return
        }

        // Trigger AI turn if applicable
        if (state.isAiMatch && state.turn == 1) {
            viewModelScope.launch {
                delay(400) // Realistic thinking delay
                val aiMove = withContext(Dispatchers.Default) {
                    AiEngine.computeBestMove(state, state.aiDifficulty)
                }

                val postAiState = GameEngine.applyMove(_gameState.value, aiMove)
                if (postAiState != null) {
                    val isWall = aiMove is Move.WallPlacement
                    soundManager.playMoveSound(isMine = false, isWall = isWall)

                    _gameState.value = postAiState
                    _validMoveHighlights.value = GameEngine.pawnMoves(postAiState, postAiState.turn)

                    if (postAiState.winner != null) {
                        soundManager.playVictoryFanfare()
                        soundManager.vibrateSuccess()
                        saveMatchToHistory(postAiState)
                    }
                }
            }
        }
    }

    fun undoMove() {
        val state = _gameState.value
        if (state.moveHistory.isEmpty() || state.isGameOver()) return

        // Undo up to 2 moves in AI mode (user move + AI move) or 1 move in local play
        val movesToPop = if (state.isAiMatch && state.moveHistory.size >= 2) 2 else 1
        var replayState = GameEngine.createInitialState(state.mode).copy(
            isAiMatch = state.isAiMatch,
            aiDifficulty = state.aiDifficulty
        )

        val targetHistory = state.moveHistory.dropLast(movesToPop)
        for (m in targetHistory) {
            val applied = GameEngine.applyMove(replayState, m)
            if (applied != null) replayState = applied
        }

        _gameState.value = replayState
        _validMoveHighlights.value = GameEngine.pawnMoves(replayState, replayState.turn)
        soundManager.vibrateShort()
    }

    fun restartGame() {
        val state = _gameState.value
        startNewGame(state.mode, _opponentType.value, state.aiDifficulty)
    }

    private fun saveMatchToHistory(state: GameState) {
        val winner = state.winner ?: return
        val durationSec = (System.currentTimeMillis() - matchStartTime) / 1000
        val wallsCount = state.walls.size

        val opponentStr = when (_opponentType.value) {
            OpponentType.AI -> "AI (${state.aiDifficulty.displayName})"
            OpponentType.LOCAL_PASS_PLAY -> "Pass & Play"
            OpponentType.ONLINE -> "Online Duel"
        }

        val record = MatchRecord(
            modeName = state.mode.displayName,
            opponentName = opponentStr,
            winnerPlayer = winner,
            totalMoves = state.moveHistory.size,
            totalWallsPlaced = wallsCount,
            durationSeconds = durationSec
        )

        viewModelScope.launch {
            repository.recordMatch(record)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
