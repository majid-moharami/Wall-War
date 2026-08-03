package com.wallwar.data.nakama

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wallwar.data.MatchRecord
import com.wallwar.data.UserProfile
import com.wallwar.model.Move
import com.wallwar.model.Position
import com.wallwar.model.Wall
import com.heroiclabs.nakama.AbstractSocketListener
import com.heroiclabs.nakama.Client
import com.heroiclabs.nakama.DefaultClient
import com.heroiclabs.nakama.DefaultSession
import com.heroiclabs.nakama.MatchData
import com.heroiclabs.nakama.MatchmakerMatched
import com.heroiclabs.nakama.PermissionRead
import com.heroiclabs.nakama.PermissionWrite
import com.heroiclabs.nakama.Session
import com.heroiclabs.nakama.SocketClient
import com.heroiclabs.nakama.StorageObjectId
import com.heroiclabs.nakama.StorageObjectWrite
import com.heroiclabs.nakama.NakamaBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class OnlineMatchEvent {
    object SearchingForMatch : OnlineMatchEvent()
    data class MatchFound(val matchId: String, val selfPlayerIndex: Int, val opponentName: String) :
        OnlineMatchEvent()

    data class OpponentMove(val move: Move) : OnlineMatchEvent()
    data class TurnTimeout(val playerIndex: Int) : OnlineMatchEvent()
    data class OpponentSurrendered(val winnerIndex: Int) : OnlineMatchEvent()
    data class MatchEnded(val winnerIndex: Int, val coinReward: Int = 75) : OnlineMatchEvent()
    data class Error(val message: String) : OnlineMatchEvent()
}

@Singleton
class NakamaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_nakama_prefs", Context.MODE_PRIVATE)

    // Config
    private val _config = MutableStateFlow(
        NakamaConfig(
            host = prefs.getString("nakama_host", "10.13.52.220") ?: "10.13.52.220",
            port = prefs.getInt("nakama_port", 7349),
            serverKey = prefs.getString("nakama_server_key", "defaultkey") ?: "defaultkey",
            useSsl = prefs.getBoolean("nakama_ssl", false)
        )
    )
    val config: StateFlow<NakamaConfig> = _config.asStateFlow()

    // Nakama SDK objects
    private var client: Client = createClient(_config.value)
    private var session: Session? = null
    private var socket: SocketClient? = null

    // Session Data
    private var nakamaUserId: String? = prefs.getString("nakama_user_id", null)
    private var nakamaUsername: String? = prefs.getString("nakama_username", null)

    // Online Status
    private val _matchState = MutableStateFlow(OnlineMatchState.IDLE)
    val matchState: StateFlow<OnlineMatchState> = _matchState.asStateFlow()

    private val _matchEvents = MutableSharedFlow<OnlineMatchEvent>(replay = 0)
    val matchEvents: SharedFlow<OnlineMatchEvent> = _matchEvents.asSharedFlow()

    // Leaderboard & Friends
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _friends = MutableStateFlow<List<NakamaFriend>>(emptyList())
    val friends: StateFlow<List<NakamaFriend>> = _friends.asStateFlow()

    // Active Match State
    private var activeMatchId: String? = null
    private var myPlayerIndex: Int = 0 
    private var currentTurnPlayer: Int = 0

    init {
        val storedToken = prefs.getString("nakama_jwt_token", null)
        if (storedToken != null) {
            session = DefaultSession.restore(storedToken, null)
            if (session?.IsExpired() == true) {
                session = null
            }
        }
    }

    private fun createClient(config: NakamaConfig): Client {
        return DefaultClient(config.serverKey, config.cleanHost, config.effectivePort, config.useSsl)
    }

    fun updateConfig(host: String, port: Int, serverKey: String, useSsl: Boolean) {
        prefs.edit()
            .putString("nakama_host", host.trim())
            .putInt("nakama_port", port)
            .putString("nakama_server_key", serverKey.trim())
            .putBoolean("nakama_ssl", useSsl)
            .apply()

        val newConfig = NakamaConfig(
            host = host.trim(),
            port = port,
            serverKey = serverKey.trim(),
            useSsl = useSsl
        )
        _config.value = newConfig
        client = createClient(newConfig)
    }

    suspend fun authenticateWithGoogle(googleIdToken: String, username: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val sanitizedUsername = username.filter { it.isLetterOrDigit() }
                
                // If we already have a session (likely a device/guest session), try to link it first
                val currentSession = session
                if (currentSession != null && !currentSession.IsExpired()) {
                    try {
                        client.linkGoogle(currentSession, googleIdToken).await()
                        Log.i("NakamaRepository", "Successfully linked Google account to current session")
                        onSessionAuthenticated()
                        return@withContext true
                    } catch (e: Exception) {
                        Log.w("NakamaRepository", "Failed to link Google account (maybe already linked to another user): ${e.message}")
                    }
                }

                // If linking failed or no current session, just authenticate normally
                session = client.authenticateGoogle(googleIdToken, true, sanitizedUsername).await()
                onSessionAuthenticated()
                true
            } catch (e: Exception) {
                Log.e("NakamaRepository", "Error in authenticateWithGoogle: ${e.message}")
                authenticateWithDevice(username)
            }
        }

    suspend fun authenticateWithDevice(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var deviceId = prefs.getString("nakama_device_id", null)
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString()
                prefs.edit().putString("nakama_device_id", deviceId).apply()
            }
            val sanitizedUsername = username.filter { it.isLetterOrDigit() }.ifBlank { "Player${(1000..9999).random()}" }
            
            session = client.authenticateDevice(deviceId, true, sanitizedUsername).await()
            onSessionAuthenticated()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Device auth error: ${e.message}")
            false
        }
    }

    private suspend fun onSessionAuthenticated() {
        session?.let {
            prefs.edit().putString("nakama_jwt_token", it.authToken).apply()
            val account = client.getAccount(it).await()
            nakamaUserId = account.user.id
            nakamaUsername = account.user.username
            prefs.edit()
                .putString("nakama_user_id", nakamaUserId)
                .putString("nakama_username", nakamaUsername)
                .apply()
        }
    }

    suspend fun ensureAuthenticatedGuest(username: String): Boolean = withContext(Dispatchers.IO) {
        if (session != null && !session!!.IsExpired()) {
            return@withContext true
        }
        return@withContext authenticateWithDevice(username)
    }

    fun getNakamaUserId(): String? = nakamaUserId

    // 2. Storage Sync (Stats, Coins, Rewards)
    suspend fun fetchUserProfileFromNakama(): JSONObject? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("stats")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            val statsObj = if (result.objectsCount > 0) {
                JSONObject(result.getObjects(0).value)
            } else {
                JSONObject()
            }

            // Also fetch account for the latest avatar_url
            try {
                val account = client.getAccount(s).await()
                if (account.user.avatarUrl != null) {
                    statsObj.put("avatarUrl", account.user.avatarUrl)
                }
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Could not fetch account info for avatar: ${e.message}")
            }

            return@withContext statsObj
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading user profile: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncUserProfileToNakama(profile: UserProfile) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val statsObj = JSONObject().apply {
                put("level", profile.level)
                put("xp", profile.xp)
                put("trophies", profile.trophies)
                put("wins", profile.wins)
                put("coins", profile.coins)
                put("rankTitle", profile.rankTitle)
                put("totalMatches", profile.totalMatches)
                put("wallsPlaced", profile.wallsPlaced)
            }

            val writeObj = StorageObjectWrite("user_data", "stats", statsObj.toString(), PermissionRead.PUBLIC_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            
            // Sync Account Profile (Display Name & Avatar)
            try {
                client.updateAccount(s, null, profile.displayName, profile.photoUrl, null, null, null).await()
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Failed to update Nakama account profile: ${e.message}")
            }

            // Post to leaderboard with avatarUrl in metadata
            val metadata = JSONObject().apply {
                if (profile.photoUrl != null) {
                    put("avatarUrl", profile.photoUrl)
                }
            }
            client.writeLeaderboardRecord(s, "global_rankings", profile.trophies.toLong(), profile.wins.toLong(), metadata.toString()).await()
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error syncing user profile: ${e.message}")
        }
    }

    suspend fun recordMatchHistoryToNakama(matchRecord: MatchRecord) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val history = fetchMatchHistoryFromNakama().toMutableList()
            history.add(0, matchRecord)
            val historyLimit = history.take(20)

            val arrayJson = JSONArray()
            for (item in historyLimit) {
                arrayJson.put(JSONObject().apply {
                    put("modeName", item.modeName)
                    put("opponentName", item.opponentName)
                    put("winnerPlayer", item.winnerPlayer)
                    put("totalMoves", item.totalMoves)
                    put("totalWallsPlaced", item.totalWallsPlaced)
                    put("durationSeconds", item.durationSeconds)
                    put("timestamp", item.timestamp)
                })
            }

            val writeObj = StorageObjectWrite("user_data", "match_history", arrayJson.toString(), PermissionRead.PUBLIC_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error recording match history: ${e.message}")
        }
    }

    suspend fun fetchMatchHistoryFromNakama(): List<MatchRecord> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext emptyList()
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("match_history")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val array = JSONArray(result.getObjects(0).value)
                val list = mutableListOf<MatchRecord>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(MatchRecord(
                        id = i.toLong(),
                        modeName = obj.optString("modeName", "Classic Duel"),
                        opponentName = obj.optString("opponentName", "Opponent"),
                        winnerPlayer = obj.optInt("winnerPlayer", 0),
                        totalMoves = obj.optInt("totalMoves", 0),
                        totalWallsPlaced = obj.optInt("totalWallsPlaced", 0),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading match history: ${e.message}")
        }
        return@withContext emptyList()
    }

    // 3. Leaderboards
    suspend fun fetchGlobalLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext emptyList()
        try {
            val result = client.listLeaderboardRecords(s, "global_rankings").await()
            val list = result.recordsList.mapIndexed { index, rec ->
                var avatarUrl: String? = null
                val metaStr = rec.metadata
                if (!metaStr.isNullOrBlank() && metaStr != "{}") {
                    try {
                        val meta = JSONObject(metaStr)
                        avatarUrl = if (meta.has("avatarUrl")) meta.getString("avatarUrl") else null
                    } catch (e: Exception) {
                        Log.w("NakamaRepository", "Error parsing leaderboard metadata: ${e.message}")
                    }
                }
                LeaderboardEntry(
                    rank = rec.rank.toInt().takeIf { it != 0 } ?: (index + 1),
                    userId = rec.ownerId,
                    username = if (rec.hasUsername()) rec.username.value else "Duelist",
                    displayName = if (rec.hasUsername()) rec.username.value else "Duelist",
                    trophies = rec.score.toInt(),
                    wins = rec.subscore.toInt(),
                    level = (rec.score.toInt() / 200) + 1,
                    avatarUrl = avatarUrl
                )
            }
            _leaderboard.value = list
            list
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error fetching leaderboard: ${e.message}")
            emptyList()
        }
    }

    // 4. Friends
    suspend fun fetchFriends(): List<NakamaFriend> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext _friends.value
        try {
            val result = client.listFriends(s).await()
            val list = result.friendsList.filter { it.state.value == 0 }.map { f ->
                NakamaFriend(
                    userId = f.user.id,
                    username = f.user.username,
                    displayName = f.user.displayName ?: f.user.username,
                    isOnline = f.user.online,
                    level = 1,
                    trophies = 0,
                    avatarUrl = f.user.avatarUrl
                )
            }
            _friends.value = list
            list
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error fetching friends: ${e.message}")
            _friends.value
        }
    }

    suspend fun addFriendByUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext false
        try {
            client.addFriends(s, username).await()
            fetchFriends()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error adding friend: ${e.message}")
            false
        }
    }

    suspend fun removeFriend(username: String): Boolean = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext false
        try {
            val friendId = _friends.value.find { it.username == username }?.userId
            if (friendId != null) {
                client.deleteFriends(s, friendId).await()
                fetchFriends()
            }
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error removing friend: ${e.message}")
            false
        }
    }

    // 5. Matchmaking & Real-time Sockets
    fun startOnlineMatchmaking(username: String) {
        scope.launch {
            _matchState.value = OnlineMatchState.CONNECTING
            _matchEvents.emit(OnlineMatchEvent.SearchingForMatch)

            if (session == null || session!!.IsExpired()) {
                if (!authenticateWithDevice(username)) {
                    _matchState.value = OnlineMatchState.ERROR
                    _matchEvents.emit(OnlineMatchEvent.Error("Authentication failed"))
                    return@launch
                }
            }

            try {
                if (socket == null) {
                    socket = client.createSocket()
                    socket?.connect(session!!, object : AbstractSocketListener() {
                        override fun onMatchmakerMatched(matched: MatchmakerMatched) {
                            handleMatchmakerMatched(matched)
                        }

                        override fun onMatchData(matchData: MatchData) {
                            handleIncomingMatchData(matchData)
                        }

                        override fun onDisconnect(t: Throwable?) {
                            Log.w("NakamaRepository", "Socket disconnected: ${t?.message}")
                            _matchState.value = OnlineMatchState.IDLE
                            socket = null
                        }
                    })?.await()
                }

                _matchState.value = OnlineMatchState.SEARCHING_MATCH
                NakamaBridge.addMatchmaker(socket!!, 2, 2, "*", null, null, 1).await()
            } catch (e: Exception) {
                Log.e("NakamaRepository", "Socket connection error: ${e.message}")
                _matchState.value = OnlineMatchState.ERROR
                _matchEvents.emit(OnlineMatchEvent.Error("Connection Error: ${e.message}"))
            }
        }
    }

    private fun handleMatchmakerMatched(matched: MatchmakerMatched) {
        scope.launch {
            try {
                val match = socket?.joinMatchToken(matched.token)?.await()
                activeMatchId = match?.matchId
                
                var oppName = "Online Opponent"
                var selfIndex = 0
                
                matched.users.forEachIndexed { index, user ->
                    if (user.presence.userId == nakamaUserId) {
                        selfIndex = index
                    } else {
                        oppName = user.presence.username ?: "Online Opponent"
                    }
                }

                myPlayerIndex = selfIndex
                _matchState.value = OnlineMatchState.IN_MATCH
                _matchEvents.emit(OnlineMatchEvent.MatchFound(activeMatchId!!, myPlayerIndex, oppName))
            } catch (e: Exception) {
                Log.e("NakamaRepository", "Error joining matched match: ${e.message}")
            }
        }
    }

    private fun handleIncomingMatchData(matchData: MatchData) {
        val opCode = matchData.opCode.toInt()
        val payload = String(matchData.data)
        val payloadJson = JSONObject(payload)

        scope.launch {
            when (opCode) {
                1 -> { // OP_PAWN_STEP
                    val r = payloadJson.getInt("r")
                    val c = payloadJson.getInt("c")
                    _matchEvents.emit(OnlineMatchEvent.OpponentMove(Move.PawnStep(Position(r, c))))
                }
                2 -> { // OP_WALL_PLACE
                    val r = payloadJson.getInt("r")
                    val c = payloadJson.getInt("c")
                    val isHorizontal = payloadJson.getBoolean("isHorizontal")
                    val owner = payloadJson.optInt("playerOwner", 1)
                    _matchEvents.emit(OnlineMatchEvent.OpponentMove(Move.WallPlacement(Wall(r, c, isHorizontal, owner))))
                }
                3 -> { // OP_TURN_TIMEOUT
                    val p = payloadJson.getInt("playerIndex")
                    _matchEvents.emit(OnlineMatchEvent.TurnTimeout(p))
                }
                4 -> { // OP_SURRENDER
                    val winnerIndex = payloadJson.getInt("winnerIndex")
                    _matchEvents.emit(OnlineMatchEvent.OpponentSurrendered(winnerIndex))
                }
            }
        }
    }

    fun sendOnlineMove(move: Move) {
        val mid = activeMatchId ?: return
        val sock = socket ?: return

        try {
            val opCode: Int
            val payload = JSONObject()

            when (move) {
                is Move.PawnStep -> {
                    opCode = 1
                    payload.put("r", move.target.r)
                    payload.put("c", move.target.c)
                }
                is Move.WallPlacement -> {
                    opCode = 2
                    payload.put("r", move.wall.r)
                    payload.put("c", move.wall.c)
                    payload.put("isHorizontal", move.wall.isHorizontal)
                    payload.put("playerOwner", myPlayerIndex)
                }
            }

            sock.sendMatchData(mid, opCode.toLong(), payload.toString().toByteArray())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending move: ${e.message}")
        }
    }

    fun sendSurrender() {
        val mid = activeMatchId ?: return
        val sock = socket ?: return

        try {
            val winnerIndex = if (myPlayerIndex == 0) 1 else 0
            val payload = JSONObject().apply {
                put("winnerIndex", winnerIndex)
            }
            sock.sendMatchData(mid, 4L, payload.toString().toByteArray())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending surrender: ${e.message}")
        }
    }

    fun cancelMatchmaking() {
        scope.launch {
            socket?.disconnectSocket()
            socket = null
            _matchState.value = OnlineMatchState.IDLE
        }
    }

    fun leaveMatch() {
        scope.launch {
            activeMatchId?.let { socket?.leaveMatch(it) }
            activeMatchId = null
            _matchState.value = OnlineMatchState.IDLE
        }
    }
}
