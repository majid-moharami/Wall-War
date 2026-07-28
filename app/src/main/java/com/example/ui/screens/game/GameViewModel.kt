package com.example.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.audio.SoundManager
import com.example.data.AuthRepository
import com.example.data.GameRepository
import com.example.data.MatchRecord
import com.example.data.SettingsRepository
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
import com.example.ui.navigation.GameBoardRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GameRepository,
    private val authRepository: AuthRepository,
    val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val navArgs = savedStateHandle.toRoute<GameBoardRoute>()

    val opponentType = try { OpponentType.valueOf(navArgs.opponent) } catch (_: Exception) { OpponentType.AI }
    val aiDifficulty = try { AiDifficulty.valueOf(navArgs.difficulty) } catch (_: Exception) { AiDifficulty.NORMAL }
    val gameMode = try { GameMode.valueOf(navArgs.mode) } catch (_: Exception) { GameMode.DUEL }

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme

    private val _gameState = MutableStateFlow(
        GameEngine.createInitialState(gameMode).copy(
            isAiMatch = opponentType == OpponentType.AI,
            aiDifficulty = aiDifficulty
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _isWallMode = MutableStateFlow(false)
    val isWallMode: StateFlow<Boolean> = _isWallMode.asStateFlow()

    private val _isWallHorizontal = MutableStateFlow(true)
    val isWallHorizontal: StateFlow<Boolean> = _isWallHorizontal.asStateFlow()

    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition.asStateFlow()

    private val _validMoveHighlights = MutableStateFlow<List<Position>>(emptyList())
    val validMoveHighlights: StateFlow<List<Position>> = _validMoveHighlights.asStateFlow()

    private var matchStartTime = System.currentTimeMillis()

    init {
        startNewGame(gameMode, opponentType, aiDifficulty)
    }

    fun startNewGame(
        mode: GameMode = gameMode,
        opponent: OpponentType = opponentType,
        difficulty: AiDifficulty = aiDifficulty
    ) {
        _isWallMode.value = false
        _selectedPosition.value = null

        val initialState = GameEngine.createInitialState(mode).copy(
            isAiMatch = opponent == OpponentType.AI,
            aiDifficulty = difficulty
        )
        _gameState.value = initialState
        updateHighlightsForState(initialState)
        matchStartTime = System.currentTimeMillis()

        checkGameEndAndTriggerAiIfNeeded(initialState)
    }

    private fun updateHighlightsForState(state: GameState) {
        if (state.isGameOver()) {
            _validMoveHighlights.value = emptyList()
            return
        }
        if (state.isAiMatch && state.turn == 1) {
            _validMoveHighlights.value = emptyList()
        } else {
            _validMoveHighlights.value = GameEngine.pawnMoves(state, state.turn)
        }
    }

    fun toggleWallMode() {
        val state = _gameState.value
        if (state.isAiMatch && state.turn == 1) return

        _isWallMode.value = !_isWallMode.value
        if (_isWallMode.value) {
            _selectedPosition.value = null
        }
        soundManager.vibrateShort()
    }

    fun selectWallOrientation(isHorizontal: Boolean) {
        val state = _gameState.value
        if (state.isAiMatch && state.turn == 1) return

        if (_isWallMode.value && _isWallHorizontal.value == isHorizontal) {
            _isWallMode.value = false
        } else {
            _isWallMode.value = true
            _isWallHorizontal.value = isHorizontal
            _selectedPosition.value = null
        }
        soundManager.vibrateShort()
    }

    fun cancelWallMode() {
        _isWallMode.value = false
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

    fun selectCell(r: Int, c: Int) {
        val state = _gameState.value
        if (state.isGameOver()) return
        if (state.isAiMatch && state.turn == 1) return

        if (_isWallMode.value) {
            val wall = Wall(r, c, _isWallHorizontal.value, playerOwner = state.turn)
            if (GameEngine.canPlaceWall(state, state.turn, wall)) {
                applyUserMove(Move.WallPlacement(wall))
                _isWallMode.value = false
            } else {
                soundManager.playErrorSound()
            }
        } else {
            val legalMoves = GameEngine.pawnMoves(state, state.turn)
            val clickedTarget = Position(r, c)
            if (legalMoves.contains(clickedTarget)) {
                applyUserMove(Move.PawnStep(clickedTarget))
            } else {
                val myPawn = state.pawns[state.turn]
                if (r == myPawn.r && c == myPawn.c) {
                    updateHighlightsForState(state)
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
        updateHighlightsForState(nextState)

        checkGameEndAndTriggerAiIfNeeded(nextState)
    }

    private fun checkGameEndAndTriggerAiIfNeeded(state: GameState) {
        if (state.winner != null) {
            soundManager.playVictoryFanfare()
            soundManager.vibrateSuccess()
            saveMatchToHistory(state)
            return
        }

        if (state.isAiMatch && state.turn == 1) {
            viewModelScope.launch {
                delay(300)
                val cur = _gameState.value
                if (cur.isGameOver() || cur.turn != 1) return@launch

                val computedMove = withContext(Dispatchers.Default) {
                    AiEngine.computeBestMove(cur, cur.aiDifficulty)
                }

                var resolvedMove = computedMove
                var postAiState = GameEngine.applyMove(_gameState.value, resolvedMove)

                // Safety fallback: if computed move was rejected, force a legal pawn step
                if (postAiState == null) {
                    val legalPawnSteps = GameEngine.pawnMoves(_gameState.value, 1)
                    if (legalPawnSteps.isNotEmpty()) {
                        resolvedMove = Move.PawnStep(legalPawnSteps.first())
                        postAiState = GameEngine.applyMove(_gameState.value, resolvedMove)
                    }
                }

                if (postAiState != null) {
                    val isWall = resolvedMove is Move.WallPlacement
                    soundManager.playMoveSound(isMine = false, isWall = isWall)

                    _gameState.value = postAiState
                    updateHighlightsForState(postAiState)

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
        updateHighlightsForState(replayState)
        soundManager.vibrateShort()
    }

    fun restartGame() {
        val state = _gameState.value
        startNewGame(state.mode, if (state.isAiMatch) OpponentType.AI else OpponentType.LOCAL_PASS_PLAY, state.aiDifficulty)
    }

    private fun saveMatchToHistory(state: GameState) {
        val winner = state.winner ?: return
        val durationSec = (System.currentTimeMillis() - matchStartTime) / 1000
        val wallsCount = state.walls.size

        val opponentStr = when {
            state.isAiMatch -> "AI (${state.aiDifficulty.displayName})"
            opponentType == OpponentType.ONLINE -> "Online Duel"
            else -> "Pass & Play"
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
            val didUserWin = (winner == 0)
            val userWallsPlaced = state.walls.count { it.playerOwner == 0 }
            authRepository.recordMatchResult(didWin = didUserWin, wallsPlaced = userWallsPlaced)
        }
    }
}
