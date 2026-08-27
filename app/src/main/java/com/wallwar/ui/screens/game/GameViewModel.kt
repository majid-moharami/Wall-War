package com.wallwar.ui.screens.game

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.audio.SoundManager
import com.wallwar.data.Arena
import com.wallwar.data.ArenaConfig
import com.wallwar.data.AuthRepository
import com.wallwar.data.DailyMissionManager
import com.wallwar.data.GameRepository
import com.wallwar.data.MatchRecord
import com.wallwar.data.MatchResultDelta
import com.wallwar.data.SettingsRepository
import com.wallwar.data.ad.AdManager
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
    private val settingsRepository: SettingsRepository,
    val adManager: AdManager,
    private val dailyMissionManager: DailyMissionManager,
    private val analyticsManager: AnalyticsManager
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

    val arenaId: String = savedStateHandle.get<String>("arenaId") ?: "pro"
    val selectedArena: Arena = ArenaConfig.getArenaById(arenaId)

    private val initialTheme: BoardTheme = if (selectedArena.id == "offline_ai") {
        when (aiDifficulty) {
            AiDifficulty.EASY -> BoardTheme.STARTER
            AiDifficulty.NORMAL -> BoardTheme.NOVICE
            AiDifficulty.PRO -> BoardTheme.MASTER
        }
    } else {
        selectedArena.boardTheme
    }

    private val _boardTheme = MutableStateFlow(initialTheme)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    val userProfile = authRepository.userProfile

    // Nakama Online State & Simulated Opponent Fallback
    private val _onlineMatchState = MutableStateFlow(nakamaRepository.matchState.value)
    val onlineMatchState: StateFlow<OnlineMatchState> = _onlineMatchState.asStateFlow()

    private val _onlineOpponentName = MutableStateFlow("Searching...")
    val onlineOpponentName: StateFlow<String> = _onlineOpponentName.asStateFlow()

    private val _myPlayerIndex = MutableStateFlow(0)
    val myPlayerIndex: StateFlow<Int> = _myPlayerIndex.asStateFlow()

    private val _opponentBallSkinId = MutableStateFlow(com.wallwar.data.BallSkinCatalog.DEFAULT_OPPONENT_BALL_ID)
    val opponentBallSkinId: StateFlow<String> = _opponentBallSkinId.asStateFlow()

    private val _opponentWallSkinId = MutableStateFlow(com.wallwar.data.WallSkinCatalog.DEFAULT_OPPONENT_WALL_ID)
    val opponentWallSkinId: StateFlow<String> = _opponentWallSkinId.asStateFlow()

    private val _onlineErrorMessage = MutableStateFlow<String?>(null)
    val onlineErrorMessage: StateFlow<String?> = _onlineErrorMessage.asStateFlow()

    private var isFakeOnlineMatch: Boolean = false
    private var fakeMatchmakerJob: kotlinx.coroutines.Job? = null
    private var fakeOpponentMoveJob: kotlinx.coroutines.Job? = null

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

    private val _turnTimeLeft = MutableStateFlow(60)
    val turnTimeLeft: StateFlow<Int> = _turnTimeLeft.asStateFlow()

    private val _isOpponentDisconnected = MutableStateFlow(false)
    val isOpponentDisconnected: StateFlow<Boolean> = _isOpponentDisconnected.asStateFlow()

    private val _disconnectSecondsRemaining = MutableStateFlow(60)
    val disconnectSecondsRemaining: StateFlow<Int> = _disconnectSecondsRemaining.asStateFlow()

    private val _isLocalDisconnected = MutableStateFlow(false)
    val isLocalDisconnected: StateFlow<Boolean> = _isLocalDisconnected.asStateFlow()

    private val _localDisconnectSeconds = MutableStateFlow(15)
    val localDisconnectSeconds: StateFlow<Int> = _localDisconnectSeconds.asStateFlow()

    private val _matchResultDelta = MutableStateFlow<MatchResultDelta?>(null)
    val matchResultDelta: StateFlow<MatchResultDelta?> = _matchResultDelta.asStateFlow()

    // Emoji Skins & Real-time In-Game Emote Reactions
    val unlockedEmojiIds: StateFlow<Set<String>> = authRepository.unlockedEmojiIds
    val allEmojis: List<com.wallwar.data.EmojiSkin> = com.wallwar.data.EmojiSkinCatalog.ALL_EMOJIS

    // Equipped Ball Skin for Custom Pawn Rendering
    val equippedBallSkinId: StateFlow<String> = authRepository.equippedBallSkinId

    // Equipped Wall Skin for Custom Wall Rendering
    val equippedWallSkinId: StateFlow<String> = authRepository.equippedWallSkinId

    private val _playerEmote = MutableStateFlow<com.wallwar.data.EmojiSkin?>(null)
    val playerEmote: StateFlow<com.wallwar.data.EmojiSkin?> = _playerEmote.asStateFlow()

    private val _opponentEmote = MutableStateFlow<com.wallwar.data.EmojiSkin?>(null)
    val opponentEmote: StateFlow<com.wallwar.data.EmojiSkin?> = _opponentEmote.asStateFlow()

    private var playerEmoteJob: kotlinx.coroutines.Job? = null
    private var opponentEmoteJob: kotlinx.coroutines.Job? = null

    private var matchStartTime: Long = System.currentTimeMillis()
    private var timerJob: kotlinx.coroutines.Job? = null
    private var disconnectTimerJob: kotlinx.coroutines.Job? = null
    private var localDisconnectJob: kotlinx.coroutines.Job? = null

    init {
        // Preload interstitial ad in the background while players are playing the game
        adManager.preloadInterstitialAd()

        val initialState = _gameState.value
        updateHighlightsForState(initialState)
        matchStartTime = System.currentTimeMillis()

        analyticsManager.logMatchStart(
            gameMode = gameMode.name,
            difficulty = if (opponentType == OpponentType.AI) aiDifficulty.name else null,
            isOnline = opponentType == OpponentType.ONLINE
        )

        if (opponentType == OpponentType.ONLINE) {
            startOnlineMatchmaking()

            viewModelScope.launch {
                nakamaRepository.matchEvents.collect { event ->
                    if (isFakeOnlineMatch) return@collect
                    when (event) {
                        is OnlineMatchEvent.MatchFound -> {
                            fakeMatchmakerJob?.cancel()
                            _onlineOpponentName.value = event.opponentName
                            _myPlayerIndex.value = event.selfPlayerIndex
                            _onlineErrorMessage.value = null
                            _isOpponentDisconnected.value = false
                            _isLocalDisconnected.value = false
                            _onlineMatchState.value = OnlineMatchState.IN_MATCH
                            
                            // Matchmaking successful: deduct entry fee now
                            if (selectedArena.entryFee > 0) {
                                authRepository.deductCoins(selectedArena.entryFee)
                                analyticsManager.logCoinsSpent(selectedArena.entryFee, "arena_entry_${selectedArena.id}")
                            }

                            // Mark match as active in persistent storage for crash/disconnect recovery
                            authRepository.markActiveOnlineMatch(event.matchId)

                            // Switch to active in-match state first
                            _onlineMatchState.value = OnlineMatchState.IN_MATCH
                            _isWallMode.value = false

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
                            // On connection or server error, show error state so user can retry or check connection
                            fakeMatchmakerJob?.cancel()
                            _onlineMatchState.value = OnlineMatchState.ERROR
                            _onlineErrorMessage.value = event.message
                        }
                        is OnlineMatchEvent.OpponentSurrendered -> {
                            val winner = _myPlayerIndex.value
                            val next = _gameState.value.copy(winner = winner)
                            _gameState.value = next
                            soundManager.playWinnerSound()
                            soundManager.playCoinSound()
                            saveMatchToHistory(next)
                            timerJob?.cancel()
                        }
                        is OnlineMatchEvent.OpponentDisconnected -> {
                            handleOpponentDisconnected()
                        }
                        is OnlineMatchEvent.OpponentReconnected -> {
                            handleOpponentReconnected()
                        }
                        is OnlineMatchEvent.OpponentEmote -> {
                            val emote = com.wallwar.data.EmojiSkinCatalog.getById(event.emojiId)
                            if (emote != null) {
                                showOpponentEmote(emote)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Handle local socket disconnection during match
            viewModelScope.launch {
                nakamaRepository.matchState.collect { state ->
                    if (!isFakeOnlineMatch) {
                        _onlineMatchState.value = state
                        if (state == OnlineMatchState.DISCONNECTED || state == OnlineMatchState.ERROR) {
                            if (_gameState.value.winner == null && !_isOpponentDisconnected.value && _onlineMatchState.value == OnlineMatchState.IN_MATCH) {
                                handleLocalConnectionLost()
                            }
                        } else if (state == OnlineMatchState.IN_MATCH) {
                            handleLocalConnectionRestored()
                        }
                    }
                }
            }
        } else {
            checkGameEndAndTriggerAiIfNeeded(initialState)
        }
    }

    private fun handleLocalConnectionLost() {
        if (_gameState.value.winner != null) return
        _isLocalDisconnected.value = true
        _localDisconnectSeconds.value = 60

        localDisconnectJob?.cancel()
        localDisconnectJob = viewModelScope.launch {
            for (sec in 60 downTo 1) {
                _localDisconnectSeconds.value = sec
                if (sec % 2 == 0 || sec == 60) {
                    nakamaRepository.attemptReconnectActiveMatch()
                }
                delay(1000)
                if (!_isLocalDisconnected.value) return@launch
            }
            if (_isLocalDisconnected.value && _gameState.value.winner == null) {
                // 60s local reconnect timeout -> forfeit loss
                val winner = 1 - _myPlayerIndex.value
                val next = _gameState.value.copy(winner = winner)
                _gameState.value = next
                soundManager.vibrateShort()
                saveMatchToHistory(next)
                _isLocalDisconnected.value = false
            }
        }
    }

    private fun handleLocalConnectionRestored() {
        _isLocalDisconnected.value = false
        localDisconnectJob?.cancel()
    }

    fun forfeitAndQuitLocalMatch() {
        if (_gameState.value.winner == null) {
            val winner = 1 - _myPlayerIndex.value
            val next = _gameState.value.copy(winner = winner)
            _gameState.value = next
            saveMatchToHistory(next)
        }
        _isLocalDisconnected.value = false
        localDisconnectJob?.cancel()
        authRepository.clearActiveOnlineMatch()
    }

    private fun handleOpponentDisconnected() {
        if (_gameState.value.winner != null) return
        _isOpponentDisconnected.value = true
        _disconnectSecondsRemaining.value = 60

        disconnectTimerJob?.cancel()
        disconnectTimerJob = viewModelScope.launch {
            for (sec in 60 downTo 1) {
                _disconnectSecondsRemaining.value = sec
                delay(1000)
                if (!_isOpponentDisconnected.value) return@launch
            }
            _disconnectSecondsRemaining.value = 0
            if (_isOpponentDisconnected.value && _gameState.value.winner == null) {
                // 1-minute timeout passed -> Remaining player wins automatically!
                val winner = _myPlayerIndex.value
                val next = _gameState.value.copy(winner = winner)
                _gameState.value = next
                soundManager.playVictoryFanfare()
                soundManager.vibrateSuccess()
                saveMatchToHistory(next)
                timerJob?.cancel()
                _isOpponentDisconnected.value = false
            }
        }
    }

    private fun handleOpponentReconnected() {
        _isOpponentDisconnected.value = false
        _disconnectSecondsRemaining.value = 60
        disconnectTimerJob?.cancel()
    }

    fun startOnlineMatchmaking() {
        fakeMatchmakerJob?.cancel()
        fakeOpponentMoveJob?.cancel()
        isFakeOnlineMatch = false
        _onlineErrorMessage.value = null
        _onlineOpponentName.value = "Searching..."
        _onlineMatchState.value = OnlineMatchState.CONNECTING

        val username = authRepository.userProfile.value.displayName
        nakamaRepository.startOnlineMatchmaking(username, selectedArena.id)

        // Random timeout between 30 and 45 seconds before falling back to a seamless fake opponent
        // ONLY trigger if currently connected and searching (not on connection error or offline)
        val fakeMatchWaitSeconds = kotlin.random.Random.nextInt(30, 46)
        fakeMatchmakerJob = viewModelScope.launch {
            delay(fakeMatchWaitSeconds * 1000L)
            if (!isFakeOnlineMatch && 
                _onlineMatchState.value == OnlineMatchState.SEARCHING_MATCH && 
                _onlineErrorMessage.value == null && 
                _gameState.value.winner == null
            ) {
                triggerFakeOnlineMatch()
            }
        }
    }

    private fun triggerFakeOnlineMatch() {
        if (_onlineMatchState.value == OnlineMatchState.IN_MATCH || 
            _onlineMatchState.value == OnlineMatchState.ERROR || 
            _onlineMatchState.value == OnlineMatchState.DISCONNECTED || 
            _onlineErrorMessage.value != null || 
            _gameState.value.winner != null
        ) return

        fakeMatchmakerJob?.cancel()
        isFakeOnlineMatch = true

        // Silently cancel Nakama matchmaking ticket in background
        nakamaRepository.cancelMatchmaking()

        // 1. Assign realistic opponent name (65% random Guest-XXXX, 35% gamer tags)
        val fakeName = generateRealisticGuestOrPlayerName()
        _onlineOpponentName.value = fakeName

        // 2. Select realistic custom cosmetic skins for the opponent:
        // Includes default blue & red balls + first 3 paid ball skins
        val paidBalls = com.wallwar.data.BallSkinCatalog.ALL_BALL_SKINS.filter { it.priceCoins > 0 }.take(3)
        val allowedBalls = listOf(
            com.wallwar.data.BallSkinCatalog.DEFAULT_BLUE,
            com.wallwar.data.BallSkinCatalog.DEFAULT_RED
        ) + paidBalls
        val randomBall = allowedBalls.random()

        // Includes default blue & red walls + first 2 paid wall skins (Cyber Matrix, Obsidian Magma)
        val paidWalls = com.wallwar.data.WallSkinCatalog.ALL_WALL_SKINS.filter { it.priceCoins > 0 }.take(2)
        val allowedWalls = listOf(
            com.wallwar.data.WallSkinCatalog.DEFAULT_BLUE,
            com.wallwar.data.WallSkinCatalog.DEFAULT_RED
        ) + paidWalls
        val randomWall = allowedWalls.random()

        _opponentBallSkinId.value = randomBall.id
        _opponentWallSkinId.value = randomWall.id

        _myPlayerIndex.value = 0
        _onlineErrorMessage.value = null
        _isOpponentDisconnected.value = false
        _isLocalDisconnected.value = false

        // 3. Deduct entry fee just like a real online match
        if (selectedArena.entryFee > 0) {
            authRepository.deductCoins(selectedArena.entryFee)
            analyticsManager.logCoinsSpent(selectedArena.entryFee, "arena_entry_${selectedArena.id}")
        }

        // 4. Switch to active in-match state & ensure wall mode is off
        _onlineMatchState.value = OnlineMatchState.IN_MATCH
        _isWallMode.value = false

        // 5. Random 50/50 starting turn (0 = player, 1 = fake opponent)
        val starterIndex = kotlin.random.Random.nextInt(2)
        val currentState = _gameState.value
        val nextState = currentState.copy(turn = starterIndex)
        _gameState.value = nextState
        updateHighlightsForState(nextState)
        startTurnTimer()

        // If fake opponent starts first, schedule their move
        if (starterIndex == 1) {
            scheduleFakeOpponentMove()
        }
    }

    private fun generateRealisticGuestOrPlayerName(): String {
        val isGuest = kotlin.random.Random.nextInt(100) < 65
        return if (isGuest) {
            "Guest-${kotlin.random.Random.nextInt(1000, 9999)}"
        } else {
            val gamerHandles = listOf(
                "ShadowStrike", "CyberKnight", "Alex_92", "NeonRacer", "PixelMaster",
                "Viper_99", "NovaPilot", "ThunderBolt", "Falcon_X", "AeroMaster",
                "PulseRider", "Quantum_8", "Krypton_9", "EchoPrime", "HyperDrive",
                "Sam_V", "ApexHunter", "ZeroCool", "SkyWalker_7", "MatrixRunner",
                "Phantom_Blade", "Zenith", "Turbo_Rex", "CosmicDrift", "Blaze_42"
            )
            gamerHandles.random()
        }
    }

    private fun scheduleFakeOpponentMove() {
        fakeOpponentMoveJob?.cancel()
        val state = _gameState.value
        if (state.winner != null || !isFakeOnlineMatch || state.turn != 1) return

        // Realistic human thinking delay between 1 to 3 seconds (timer runs naturally)
        val thinkingDelay = kotlin.random.Random.nextLong(1000, 3001)

        fakeOpponentMoveJob = viewModelScope.launch {
            delay(thinkingDelay)
            if (_gameState.value.turn == 1 && _gameState.value.winner == null && isFakeOnlineMatch) {
                val current = _gameState.value
                val effectiveDiff = when (selectedArena.id) {
                    "starter" -> AiDifficulty.NORMAL
                    "rookie" -> AiDifficulty.NORMAL
                    "champion" -> AiDifficulty.PRO
                    "grandmaster" -> AiDifficulty.PRO
                    else -> AiDifficulty.NORMAL
                }
                val move = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    AiEngine.computeBestMove(current, effectiveDiff)
                }
                applyRemoteMove(move)

                // 25% chance for the fake opponent to send a reaction emote
                if (kotlin.random.Random.nextInt(100) < 25 && allEmojis.isNotEmpty()) {
                    delay(kotlin.random.Random.nextLong(1200, 2500))
                    if (_gameState.value.winner == null && isFakeOnlineMatch) {
                        showOpponentEmote(allEmojis.random())
                    }
                }
            }
        }
    }

    fun cancelOnlineMatchmaking() {
        fakeMatchmakerJob?.cancel()
        fakeOpponentMoveJob?.cancel()
        isFakeOnlineMatch = false
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
            if (opponentType == OpponentType.ONLINE && !isFakeOnlineMatch) {
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
                if (opponentType == OpponentType.ONLINE && !isFakeOnlineMatch) {
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
            if (opponentType == OpponentType.ONLINE && !isFakeOnlineMatch) {
                nakamaRepository.sendOnlineMove(move)
            }
            _selectedPosition.value = null
        } else {
            _selectedPosition.value = target
        }
    }

    private fun applyUserMove(move: Move) {
        val currentState = _gameState.value
        val playerIdx = if (opponentType == OpponentType.ONLINE) _myPlayerIndex.value else currentState.turn
        val nextState = GameEngine.applyMove(currentState, move, playerIdx) ?: return

        val isWall = move is Move.WallPlacement
        soundManager.playMoveSound(isMine = true, isWall = isWall)
        soundManager.vibrateShort()

        _gameState.value = nextState
        startTurnTimer()

        if (isFakeOnlineMatch) {
            updateHighlightsForState(nextState)
            if (nextState.winner != null) {
                timerJob?.cancel()
                if (nextState.winner == _myPlayerIndex.value) {
                    soundManager.playWinnerSound()
                    soundManager.playCoinSound()
                } else {
                    soundManager.playLoserSound()
                }
                saveMatchToHistory(nextState)
            } else if (nextState.turn == 1) {
                scheduleFakeOpponentMove()
            }
        } else {
            checkGameEndAndTriggerAiIfNeeded(nextState)
        }
    }

    private fun applyRemoteMove(move: Move) {
        val currentState = _gameState.value
        val oppIndex = 1 - _myPlayerIndex.value
        val nextState = GameEngine.applyMove(currentState, move, oppIndex) ?: return

        val isWall = move is Move.WallPlacement
        soundManager.playMoveSound(isMine = false, isWall = isWall)

        _gameState.value = nextState
        updateHighlightsForState(nextState)
        startTurnTimer()

        if (nextState.winner != null) {
            timerJob?.cancel()
            if (nextState.winner == _myPlayerIndex.value) {
                soundManager.playWinnerSound()
                soundManager.playCoinSound()
            } else {
                soundManager.playLoserSound()
            }
            saveMatchToHistory(nextState)
        }
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        if (opponentType != OpponentType.ONLINE) {
            return
        }
        _turnTimeLeft.value = 60
        
        val isLocalTurn = _gameState.value.turn == _myPlayerIndex.value

        if (_gameState.value.winner != null) return

        timerJob = viewModelScope.launch {
            while (_turnTimeLeft.value > 0) {
                delay(1000)
                _turnTimeLeft.value -= 1
            }
            
            // Time up!
            if (isLocalTurn) {
                if (!isFakeOnlineMatch) {
                    nakamaRepository.sendTurnTimeout()
                }
                switchTurnLocally()
            } else if (isFakeOnlineMatch) {
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

        if (isFakeOnlineMatch && nextState.turn == 1 && nextState.winner == null) {
            scheduleFakeOpponentMove()
        }
    }

    private fun checkGameEndAndTriggerAiIfNeeded(state: GameState) {
        updateHighlightsForState(state)

        if (state.winner != null) {
            if (state.winner == _myPlayerIndex.value) {
                soundManager.playWinnerSound()
                soundManager.playCoinSound()
            } else {
                soundManager.playLoserSound()
            }
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
                            soundManager.playWinnerSound()
                            soundManager.playCoinSound()
                        } else {
                            soundManager.playLoserSound()
                        }
                        saveMatchToHistory(afterAiState)
                    }
                }
            }
        }
    }

    fun resignGame() {
        if (opponentType == OpponentType.ONLINE && !isFakeOnlineMatch) {
            nakamaRepository.sendSurrender()
        }
        soundManager.playLoserSound()
        val winner = 1 - _myPlayerIndex.value
        val next = _gameState.value.copy(winner = winner)
        _gameState.value = next
        saveMatchToHistory(next)
        timerJob?.cancel()
        fakeOpponentMoveJob?.cancel()
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
        // Preload interstitial ad in the background for the next match ending
        adManager.preloadInterstitialAd()
        _matchResultDelta.value = null

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

    fun showMatchEndInterstitial(activity: Activity? = null, onClosed: () -> Unit) {
        adManager.showInterstitialIfTriggered(activity = activity, onAdClosed = onClosed)
    }

    private fun saveMatchToHistory(finalState: GameState) {
        val winnerIndex = finalState.winner ?: return
        val didWin = winnerIndex == _myPlayerIndex.value
        val wallsPlacedCount = finalState.moveHistory.count { it is Move.WallPlacement && (it.wall.playerOwner == _myPlayerIndex.value) }
        val durationSeconds = (System.currentTimeMillis() - matchStartTime) / 1000
        val totalMoves = finalState.moveHistory.size
        val currentStreak = authRepository.userProfile.value.currentWinStreak

        // Track completed matches in AdManager for Interstitial rule (every 2 matches)
        adManager.recordMatchCompleted()

        // Log match analytics
        analyticsManager.logMatchEnd(
            gameMode = finalState.mode.name,
            isWin = didWin,
            winnerName = if (didWin) "LocalPlayer" else "Opponent",
            durationSeconds = durationSeconds,
            turnsCount = totalMoves,
            wallsPlaced = wallsPlacedCount
        )

        // Track Daily Missions
        dailyMissionManager.recordMatchPlayed(
            didWin = didWin,
            opponentType = opponentType,
            aiDifficulty = aiDifficulty,
            wallsPlaced = wallsPlacedCount,
            totalMoves = totalMoves,
            durationSeconds = durationSeconds,
            arenaId = selectedArena.id,
            currentWinStreak = if (didWin) currentStreak + 1 else 0
        )

        if (opponentType == OpponentType.ONLINE) {
            val opponentName = _onlineOpponentName.value

            viewModelScope.launch {
                val record = MatchRecord(
                    modeName = "${selectedArena.title} (${finalState.mode.displayName})",
                    opponentName = opponentName,
                    winnerPlayer = if (didWin) 0 else 1,
                    totalMoves = finalState.moveHistory.size,
                    totalWallsPlaced = finalState.walls.size,
                    durationSeconds = durationSeconds
                )
                gameRepository.recordMatch(record)
                
                // Update local profile with arena payouts and sync to Nakama (online match)
                val delta = authRepository.recordArenaMatchResult(
                    didWin = didWin,
                    wallsPlaced = wallsPlacedCount,
                    winningPrize = selectedArena.winningPrize,
                    isOnline = true
                )
                _matchResultDelta.value = delta
                authRepository.clearActiveOnlineMatch()
            }
        } else if (opponentType == OpponentType.AI || opponentType == OpponentType.LOCAL_PASS_PLAY) {
            // Practice / AI match: award training rewards without altering ranked history or trophy stats
            val delta = authRepository.recordArenaMatchResult(
                didWin = didWin,
                wallsPlaced = wallsPlacedCount,
                winningPrize = selectedArena.winningPrize,
                isOnline = false
            )
            _matchResultDelta.value = delta
        }
    }

    fun sendEmote(emoji: com.wallwar.data.EmojiSkin) {
        if (!unlockedEmojiIds.value.contains(emoji.id)) return

        _playerEmote.value = emoji
        soundManager.playButtonClick()

        playerEmoteJob?.cancel()
        playerEmoteJob = viewModelScope.launch {
            delay(2000)
            if (_playerEmote.value?.id == emoji.id) {
                _playerEmote.value = null
            }
        }

        if (opponentType == OpponentType.ONLINE) {
            if (!isFakeOnlineMatch) {
                nakamaRepository.sendOnlineEmote(emoji.id, emoji.symbol)
            } else {
                // Fake online opponent has a 45% chance to react back with an emote after 1.8 - 3.8 seconds
                if (kotlin.random.Random.nextInt(100) < 45) {
                    viewModelScope.launch {
                        delay(kotlin.random.Random.nextLong(1800, 3800))
                        if (_gameState.value.winner == null && isFakeOnlineMatch && allEmojis.isNotEmpty()) {
                            showOpponentEmote(allEmojis.random())
                        }
                    }
                }
            }
        } else if (opponentType == OpponentType.AI) {
            // Bot AI reacts dynamically back with an emote
            viewModelScope.launch {
                delay(1200)
                val botResponses = allEmojis.filter { it.id != emoji.id }
                if (botResponses.isNotEmpty()) {
                    showOpponentEmote(botResponses.random())
                }
            }
        }
    }

    fun showOpponentEmote(emoji: com.wallwar.data.EmojiSkin) {
        _opponentEmote.value = emoji
        soundManager.playButtonClick()

        opponentEmoteJob?.cancel()
        opponentEmoteJob = viewModelScope.launch {
            delay(2000)
            if (_opponentEmote.value?.id == emoji.id) {
                _opponentEmote.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fakeMatchmakerJob?.cancel()
        fakeOpponentMoveJob?.cancel()
        timerJob?.cancel()
        disconnectTimerJob?.cancel()
        playerEmoteJob?.cancel()
        opponentEmoteJob?.cancel()
        if (opponentType == OpponentType.ONLINE && !isFakeOnlineMatch) {
            nakamaRepository.leaveMatch()
        }
    }
}
