package com.wallwar.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.GameRepository
import com.wallwar.data.MatchRecord
import com.wallwar.data.SettingsRepository
import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.data.nakama.OnlineMatchEvent
import com.wallwar.data.nakama.OnlineMatchState
import com.wallwar.engine.AiEngine
import com.wallwar.engine.GameEngine
import com.wallwar.model.AiDifficulty
import com.wallwar.model.BoardTheme
import com.wallwar.model.GameMode
import com.wallwar.model.GameState
import com.wallwar.model.Move
import com.wallwar.model.OpponentType
import com.wallwar.model.Position
import com.wallwar.model.Wall
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val nakamaRepository: NakamaRepository,
    val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val gameMode: GameMode = try {
        GameMode.valueOf(savedStateHandle.get<String>("mode") ?: GameMode.DUEL.name)
    } catch (_: Exception) {
        GameMode.DUEL
    }

    val opponentType: OpponentType = try {
        OpponentType.valueOf(savedStateHandle.get<String>("opponent") ?: OpponentType.AI.name)
    } catch (_: Exception) {
        OpponentType.AI
    }

    val aiDifficulty: AiDifficulty = try {
        AiDifficulty.valueOf(savedStateHandle.get<String>("difficulty") ?: AiDifficulty.NORMAL.name)
    } catch (_: Exception) {
        AiDifficulty.NORMAL
    }

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme

    val userProfile = authRepository.userProfile

    // Nakama Online State
    val onlineMatchState: StateFlow<OnlineMatchState> = nakamaRepository.matchState

    private val _onlineOpponentName = MutableStateFlow("Searching...")
    val onlineOpponentName: StateFlow<String> = _onlineOpponentName.asStateFlow()

    private val _myPlayerIndex = MutableStateFlow(0)
    val myPlayerIndex: StateFlow<Int> = _myPlayerIndex.asStateFlow()

    private val _onlineErrorMessage = MutableStateFlow<String?>(null)
    val onlineErrorMessage: StateFlow<String?> = _onlineErrorMessage.asStateFlow()

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

    private val _turnTimeLeft = MutableStateFlow(30)
    val turnTimeLeft: StateFlow<Int> = _turnTimeLeft.asStateFlow()

    private var matchStartTime: Long = System.currentTimeMillis()
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        val initialState = _gameState.value
        updateHighlightsForState(initialState)
        matchStartTime = System.currentTimeMillis()

        if (opponentType == OpponentType.ONLINE) {
            startOnlineMatchmaking()

            viewModelScope.launch {
                nakamaRepository.matchEvents.collect { event ->
                    when (event) {
                        is OnlineMatchEvent.MatchFound -> {
                            _onlineOpponentName.value = event.opponentName
                            _myPlayerIndex.value = event.selfPlayerIndex
                            _onlineErrorMessage.value = null
                            
                            // Set the initial turn based on the deterministic starter index
                            val currentState = _gameState.value
                            val nextState = currentState.copy(turn = event.starterIndex)
                            _gameState.value = nextState
                            updateHighlightsForState(nextState)
                            startTurnTimer()
                        }
                        is OnlineMatchEvent.OpponentMove -> {
                            applyRemoteMove(event.move)
                        }
                        is OnlineMatchEvent.TurnTimeout -> {
                            handleRemoteTimeout()
                        }
                        is OnlineMatchEvent.Error -> {
                            _onlineErrorMessage.value = event.message
                        }
                        is OnlineMatchEvent.OpponentSurrendered -> {
                            val winner = if (_myPlayerIndex.value == 0) 0 else 1
                            val next = _gameState.value.copy(winner = winner)
                            _gameState.value = next
                            soundManager.playVictoryFanfare()
                            saveMatchToHistory(next)
                        }
                        else -> {}
                    }
                }
            }
        } else {
            checkGameEndAndTriggerAiIfNeeded(initialState)
        }
    }

    fun startOnlineMatchmaking() {
        _onlineErrorMessage.value = null
        val username = authRepository.userProfile.value.displayName
        nakamaRepository.startOnlineMatchmaking(username)
    }

    fun cancelOnlineMatchmaking() {
        if (opponentType == OpponentType.ONLINE) {
            nakamaRepository.cancelMatchmaking()
        }
    }

    private fun updateHighlightsForState(state: GameState) {
        if (state.isGameOver()) {
            _validMoveHighlights.value = emptyList()
            return
        }

        // If AI's turn, clear user highlights
        if (state.isAiMatch && state.turn == 1) {
            _validMoveHighlights.value = emptyList()
            return
        }

        // If Online match and NOT my turn, clear highlights
        if (opponentType == OpponentType.ONLINE) {
            if (onlineMatchState.value != OnlineMatchState.IN_MATCH || state.turn != _myPlayerIndex.value) {
                _validMoveHighlights.value = emptyList()
                return
            }
        }

        _validMoveHighlights.value = GameEngine.pawnMoves(state, state.turn)
    }

    fun toggleWallMode() {
        val state = _gameState.value
        if (state.isAiMatch && state.turn == 1) return
        if (opponentType == OpponentType.ONLINE && state.turn != _myPlayerIndex.value) return

        _isWallMode.value = !_isWallMode.value
        if (_isWallMode.value) {
            _selectedPosition.value = null
        }
    }

    fun selectWallOrientation(isHorizontal: Boolean) {
        val state = _gameState.value
        if (state.isAiMatch && state.turn == 1) return
        if (opponentType == OpponentType.ONLINE && state.turn != _myPlayerIndex.value) return

        if (_isWallMode.value && _isWallHorizontal.value == isHorizontal) {
            _isWallMode.value = false
        } else {
            _isWallMode.value = true
            _isWallHorizontal.value = isHorizontal
            _selectedPosition.value = null
        }
    }

    fun placeWall(r: Int, c: Int, isHorizontal: Boolean) {
        val state = _gameState.value
        if (state.isGameOver()) return
        if (state.isAiMatch && state.turn == 1) return
        if (opponentType == OpponentType.ONLINE && (onlineMatchState.value != OnlineMatchState.IN_MATCH || state.turn != _myPlayerIndex.value)) return

        val wall = Wall(r, c, isHorizontal, playerOwner = state.turn)
        if (GameEngine.canPlaceWall(state, state.turn, wall)) {
            val move = Move.WallPlacement(wall)
            applyUserMove(move)
            if (opponentType == OpponentType.ONLINE) {
                nakamaRepository.sendOnlineMove(move)
            }
            _isWallMode.value = false
        } else {
            soundManager.playErrorSound()
        }
    }

    fun selectCell(r: Int, c: Int) {
        val state = _gameState.value
        if (state.isGameOver()) return
        if (state.isAiMatch && state.turn == 1) return
        if (opponentType == OpponentType.ONLINE && (onlineMatchState.value != OnlineMatchState.IN_MATCH || state.turn != _myPlayerIndex.value)) return

        if (_isWallMode.value) {
            val wall = Wall(r, c, _isWallHorizontal.value, playerOwner = state.turn)
            if (GameEngine.canPlaceWall(state, state.turn, wall)) {
                val move = Move.WallPlacement(wall)
                applyUserMove(move)
                if (opponentType == OpponentType.ONLINE) {
                    nakamaRepository.sendOnlineMove(move)
                }
                _isWallMode.value = false
            } else {
                soundManager.playErrorSound()
            }
            return
        }

        val target = Position(r, c)
        val pPos = state.pawns[state.turn]

        if (target == pPos) {
            _selectedPosition.value = if (_selectedPosition.value == target) null else target
            return
        }

        if (_validMoveHighlights.value.contains(target)) {
            val move = Move.PawnStep(target)
            applyUserMove(move)
            if (opponentType == OpponentType.ONLINE) {
                nakamaRepository.sendOnlineMove(move)
            }
            _selectedPosition.value = null
        } else {
            _selectedPosition.value = target
        }
    }

    private fun applyUserMove(move: Move) {
        val currentState = _gameState.value
        val nextState = GameEngine.applyMove(currentState, move) ?: return

        val isWall = move is Move.WallPlacement
        soundManager.playMoveSound(isMine = true, isWall = isWall)
        soundManager.vibrateShort()

        _gameState.value = nextState
        startTurnTimer()

        checkGameEndAndTriggerAiIfNeeded(nextState)
    }

    private fun applyRemoteMove(move: Move) {
        val currentState = _gameState.value
        val nextState = GameEngine.applyMove(currentState, move) ?: return

        val isWall = move is Move.WallPlacement
        soundManager.playMoveSound(isMine = false, isWall = isWall)

        _gameState.value = nextState
        updateHighlightsForState(nextState)
        startTurnTimer()

        if (nextState.winner != null) {
            if (nextState.winner == _myPlayerIndex.value) {
                soundManager.playVictoryFanfare()
                soundManager.vibrateSuccess()
            } else {
                soundManager.playErrorSound()
            }
            saveMatchToHistory(nextState)
        }
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        _turnTimeLeft.value = 30
        
        val isLocalTurn = if (opponentType == OpponentType.ONLINE) {
            _gameState.value.turn == _myPlayerIndex.value
        } else {
            // Local game (AI or Pass & Play)
            !(_gameState.value.isAiMatch && _gameState.value.turn == 1)
        }

        if (_gameState.value.winner != null) return

        timerJob = viewModelScope.launch {
            while (_turnTimeLeft.value > 0) {
                delay(1000)
                _turnTimeLeft.value -= 1
            }
            
            // Time up!
            if (isLocalTurn && opponentType == OpponentType.ONLINE) {
                nakamaRepository.sendTurnTimeout()
                switchTurnLocally()
            } else if (opponentType == OpponentType.LOCAL_PASS_PLAY) {
                switchTurnLocally()
            }
        }
    }

    private fun handleRemoteTimeout() {
        if (_gameState.value.turn != _myPlayerIndex.value) {
            switchTurnLocally()
        }
    }

    private fun switchTurnLocally() {
        val currentState = _gameState.value
        if (currentState.winner != null) return
        
        val nextState = currentState.copy(turn = 1 - currentState.turn)
        _gameState.value = nextState
        updateHighlightsForState(nextState)
        startTurnTimer()
    }

    private fun checkGameEndAndTriggerAiIfNeeded(state: GameState) {
        updateHighlightsForState(state)

        if (state.winner != null) {
            soundManager.playVictoryFanfare()
            soundManager.vibrateSuccess()
            saveMatchToHistory(state)
            return
        }

        if (state.isAiMatch && state.turn == 1) {
            viewModelScope.launch {
                delay(400) // Small delay for realistic feel
                val aiMove = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    AiEngine.computeBestMove(state, state.aiDifficulty)
                }
                val afterAiState = GameEngine.applyMove(_gameState.value, aiMove)
                if (afterAiState != null) {
                    val isWall = aiMove is Move.WallPlacement
                    soundManager.playMoveSound(isMine = false, isWall = isWall)
                    _gameState.value = afterAiState
                    updateHighlightsForState(afterAiState)

                    if (afterAiState.winner != null) {
                        if (afterAiState.winner == 0) {
                            soundManager.playVictoryFanfare()
                            soundManager.vibrateSuccess()
                        } else {
                            soundManager.playErrorSound()
                        }
                        saveMatchToHistory(afterAiState)
                    }
                }
            }
        }
    }

    fun undoMove() {
        if (opponentType == OpponentType.ONLINE) return // No undo in online multiplayer

        val state = _gameState.value
        if (state.moveHistory.isEmpty()) return

        val movesToKeep = if (state.isAiMatch && state.moveHistory.size >= 2) {
            state.moveHistory.size - 2
        } else {
            state.moveHistory.size - 1
        }

        var newState = GameEngine.createInitialState(gameMode).copy(
            isAiMatch = opponentType == OpponentType.AI,
            aiDifficulty = aiDifficulty
        )

        for (i in 0 until movesToKeep) {
            val move = state.moveHistory[i]
            val applied = GameEngine.applyMove(newState, move)
            if (applied != null) {
                newState = applied
            }
        }

        soundManager.vibrateShort()
        _gameState.value = newState
        _isWallMode.value = false
        _selectedPosition.value = null
        updateHighlightsForState(newState)
    }

    fun restartGame() {
        if (opponentType == OpponentType.ONLINE) {
            startOnlineMatchmaking()
            return
        }

        soundManager.vibrateShort()
        val newState = GameEngine.createInitialState(gameMode).copy(
            isAiMatch = opponentType == OpponentType.AI,
            aiDifficulty = aiDifficulty
        )
        _gameState.value = newState
        _isWallMode.value = false
        _selectedPosition.value = null
        matchStartTime = System.currentTimeMillis()
        updateHighlightsForState(newState)

        checkGameEndAndTriggerAiIfNeeded(newState)
    }

    private fun saveMatchToHistory(finalState: GameState) {
        if (opponentType != OpponentType.ONLINE) {
            return // Offline games are not calculated or saved in match history
        }

        val winnerIndex = finalState.winner ?: return
        val opponentName = _onlineOpponentName.value
        val durationSeconds = (System.currentTimeMillis() - matchStartTime) / 1000

        viewModelScope.launch {
            val record = MatchRecord(
                modeName = finalState.mode.displayName,
                opponentName = opponentName,
                winnerPlayer = winnerIndex,
                totalMoves = finalState.moveHistory.size,
                totalWallsPlaced = finalState.walls.size,
                durationSeconds = durationSeconds
            )
            gameRepository.recordMatch(record)
            nakamaRepository.recordMatchHistoryToNakama(record)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (opponentType == OpponentType.ONLINE) {
            nakamaRepository.leaveMatch()
        }
    }
}
