package com.wallwar.data.nakama

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wallwar.data.MatchRecord
import com.wallwar.data.UserProfile
import com.wallwar.model.BoardTheme
import com.wallwar.model.Move
import com.wallwar.model.Position
import com.wallwar.model.Wall
import com.wallwar.ui.screens.shop.CoinPack
import com.heroiclabs.nakama.AbstractSocketListener
import com.heroiclabs.nakama.Client
import com.heroiclabs.nakama.DefaultClient
import com.heroiclabs.nakama.DefaultSession
import com.heroiclabs.nakama.MatchData
import com.heroiclabs.nakama.MatchmakerMatched
import com.heroiclabs.nakama.MatchPresenceEvent
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
    data class MatchFound(val matchId: String, val selfPlayerIndex: Int, val opponentName: String, val starterIndex: Int) :
        OnlineMatchEvent()

    data class OpponentMove(val move: Move) : OnlineMatchEvent()
    data class OpponentEmote(val emojiId: String, val emojiSymbol: String, val playerIndex: Int) : OnlineMatchEvent()
    data class TurnTimeout(val playerIndex: Int) : OnlineMatchEvent()
    data class OpponentSurrendered(val winnerIndex: Int) : OnlineMatchEvent()
    object OpponentDisconnected : OnlineMatchEvent()
    object OpponentReconnected : OnlineMatchEvent()
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

    companion object {
        const val DEFAULT_HOST = "https://nakama.wallwargame.com"
        const val DEFAULT_PORT = 7349
        const val DEFAULT_SERVER_KEY = "uReirVWP9KAKwsi96zmsB2iDEKCUELzT"
        private val IPV4_PATTERN = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?$")
    }

    // Config
    private val _config = MutableStateFlow(
        run {
            val savedHost = prefs.getString("nakama_host", null)?.trim()
            val isLegacyIp = savedHost == null || savedHost.isBlank() || IPV4_PATTERN.matches(savedHost)
            val host = if (isLegacyIp) {
                DEFAULT_HOST
            } else {
                savedHost!!
            }
            val rawPort = prefs.getInt("nakama_port", DEFAULT_PORT)
            val port = if (isLegacyIp || rawPort == 7351 || rawPort == 7350) {
                DEFAULT_PORT
            } else {
                rawPort
            }
            val savedKey = prefs.getString("nakama_server_key", null)
            val serverKey = if (savedKey == null || savedKey == "defaultkey" || savedKey.isBlank()) {
                DEFAULT_SERVER_KEY
            } else {
                savedKey
            }
            val useSsl = if (isLegacyIp) {
                true
            } else {
                prefs.getBoolean("nakama_ssl", host.startsWith("https://") || host.startsWith("wss://"))
            }
            NakamaConfig(
                host = host,
                port = port,
                serverKey = serverKey,
                useSsl = useSsl
            )
        }
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
        return DefaultClient(config.serverKey, config.cleanHost, config.effectivePort, config.effectiveSsl)
    }

    fun updateConfig(host: String, port: Int, serverKey: String, useSsl: Boolean) {
        val lower = host.trim().lowercase()
        val effectiveSsl = if (lower.startsWith("http://") || lower.startsWith("ws://")) {
            false
        } else if (lower.startsWith("https://") || lower.startsWith("wss://")) {
            true
        } else {
            useSsl
        }
        
        prefs.edit()
            .putString("nakama_host", host.trim())
            .putInt("nakama_port", port)
            .putString("nakama_server_key", serverKey.trim())
            .putBoolean("nakama_ssl", effectiveSsl)
            .apply()

        val newConfig = NakamaConfig(
            host = host.trim(),
            port = port,
            serverKey = serverKey.trim(),
            useSsl = effectiveSsl
        )
        _config.value = newConfig
        client = createClient(newConfig)
    }

    fun hasValidSession(): Boolean {
        val s = session
        return s != null && !s.IsExpired()
    }

    fun logout() {
        session = null
        nakamaUserId = null
        nakamaUsername = null
        prefs.edit()
            .remove("nakama_jwt_token")
            .remove("nakama_user_id")
            .remove("nakama_username")
            .apply()

        scope.launch {
            try {
                socket?.disconnectSocket()
                socket = null
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Socket disconnect on logout: ${e.message}")
            }
        }
    }

    suspend fun authenticateWithEmail(
        email: String,
        password: String,
        create: Boolean,
        username: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // When creating, the user's email is their username on the Nakama server (sanitized for nakama requirement)
            val desiredUsername = if (create) {
                // If username is provided, use it, or default to the email address
                username?.trim()?.ifBlank { null } ?: email.trim()
            } else {
                null
            }
            session = if (!desiredUsername.isNullOrBlank()) {
                client.authenticateEmail(email.trim(), password.trim(), create, desiredUsername).await()
            } else {
                client.authenticateEmail(email.trim(), password.trim(), create).await()
            }
            onSessionAuthenticated()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error in authenticateWithEmail (create=$create): ${e.message}", e)
            val msg = e.localizedMessage ?: e.message ?: "Authentication failed"
            val errorDetails = when {
                msg.contains("password", ignoreCase = true) || msg.contains("invalid", ignoreCase = true) || msg.contains("unauthenticated", ignoreCase = true) ->
                    if (create) "Registration failed. Password must be at least 8 characters." else "Invalid email or password."
                msg.contains("already", ignoreCase = true) || msg.contains("exists", ignoreCase = true) || msg.contains("username", ignoreCase = true) ->
                    "An account with this email/username already exists. Try logging in instead."
                msg.contains("connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) || msg.contains("host", ignoreCase = true) ->
                    "Unable to connect to Nakama Server. Check network connection or server settings."
                else -> msg
            }
            throw IllegalArgumentException(errorDetails)
        }
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
                ensureAuthenticatedGuest(username)
            }
        }

    suspend fun authenticateWithDevice(deviceId: String, username: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString("nakama_device_id", deviceId).apply()
            val sanitizedUsername = username?.filter { it.isLetterOrDigit() }?.ifBlank { null }
            
            session = try {
                if (sanitizedUsername != null) {
                    client.authenticateDevice(deviceId, true, sanitizedUsername).await()
                } else {
                    client.authenticateDevice(deviceId, true).await()
                }
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Device auth with username '$sanitizedUsername' failed: ${e.message}, retrying without username")
                client.authenticateDevice(deviceId, true).await()
            }
            
            onSessionAuthenticated()
            true
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Device auth not available: ${e.message}")
            false
        }
    }

    suspend fun testConnectionDetailed(deviceId: String = "TestDevice"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val cfg = _config.value
            val testClient = createClient(cfg)
            val testSession = testClient.authenticateDevice(deviceId, true).await()
            if (testSession != null && !testSession.authToken.isNullOrBlank()) {
                Pair(true, "Connected successfully!\nServer: ${cfg.httpBaseUrl}\nSession token acquired for User: ${testSession.userId ?: "Connected"}")
            } else {
                Pair(false, "Server responded with empty session token at ${cfg.httpBaseUrl}")
            }
        } catch (e: Throwable) {
            Log.e("NakamaRepository", "Test connection failed: ${e.message}", e)
            val cfg = _config.value
            val exName = e.javaClass.simpleName
            val exMsg = e.localizedMessage ?: e.message ?: "Unknown error"
            val cause = e.cause?.let { "\nCause: [${it.javaClass.simpleName}] ${it.message ?: it.localizedMessage}" } ?: ""
            
            val diagnosticHint = when {
                cause.contains("TLSV1_ALERT_NO_APPLICATION_PROTOCOL", ignoreCase = true) || exMsg.contains("NO_APPLICATION_PROTOCOL", ignoreCase = true) ->
                    "⚠️ TLS / ALPN Protocol Error: The server at ${cfg.cleanHost}:${cfg.effectivePort} did not negotiate gRPC HTTP/2 (ALPN h2).\n" +
                    "• If connecting directly to Nakama (e.g. docker port 7349/7350), turn 'Use SSL' OFF (or use http://).\n" +
                    "• If using an HTTPS reverse proxy (Nginx, Cloudflare, Traefik, Caddy), set Port to 443 with SSL ON and enable HTTP/2 / gRPC in your reverse proxy config.\n" +
                    "• Nakama Java SDK default gRPC port is 7349."
                cfg.effectivePort == 7351 && exName.contains("SSL", ignoreCase = true) ->
                    "⚠️ Protocol mismatch on Port 7351: Port 7351 is Nakama's Web Management Console (browser dashboard). For game client connections, switch the port to 7349 (gRPC), 7350 (API), or 443 (HTTPS reverse proxy)."
                exMsg.contains("Failed to connect", ignoreCase = true) || exMsg.contains("Connection refused", ignoreCase = true) ->
                    "Hint: Connection refused on port ${cfg.effectivePort}. Ensure Nakama server or reverse proxy is running and port ${cfg.effectivePort} is accessible."
                exMsg.contains("Unable to resolve host", ignoreCase = true) || exName.contains("UnknownHost", ignoreCase = true) ->
                    "Hint: DNS lookup failed for host '${cfg.cleanHost}'. Check internet connection and DNS settings."
                exName.contains("SSL", ignoreCase = true) || exMsg.contains("SSL", ignoreCase = true) || exMsg.contains("Cert", ignoreCase = true) ->
                    "Hint: SSL/TLS handshake failed. If connecting to a local/raw port (7349/7350), try turning SSL OFF. If using HTTPS, verify valid certificate on port 443."
                exMsg.contains("401") || exMsg.contains("Unauthorized") || exMsg.contains("invalid key", ignoreCase = true) ->
                    "Hint: HTTP 401 Unauthorized. Verify server key (default is '${DEFAULT_SERVER_KEY}')."
                exMsg.contains("404") || exMsg.contains("Not Found") ->
                    "Hint: HTTP 404 Not Found at port ${cfg.effectivePort}. The game client connects to port 7349, 7350, or 443."
                exMsg.contains("502") || exMsg.contains("Bad Gateway") ->
                    "Hint: HTTP 502 Bad Gateway. Reverse proxy is reachable but backend Nakama instance is down."
                else -> ""
            }
            
            val fullError = buildString {
                append("Target: ${cfg.httpBaseUrl}\n")
                append("Error: [$exName] $exMsg$cause")
                if (diagnosticHint.isNotBlank()) {
                    append("\n\n$diagnosticHint")
                }
            }
            Pair(false, fullError)
        }
    }

    suspend fun linkGoogle(idToken: String): Boolean = withContext(Dispatchers.IO) {
        val currentSession = session ?: throw IllegalStateException("No active Nakama session to link")
        try {
            client.linkGoogle(currentSession, idToken).await()
            onSessionAuthenticated()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Failed to link Google account: ${e.message}", e)
            throw e
        }
    }

    suspend fun linkEmail(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val currentSession = session ?: throw IllegalStateException("No active Nakama session to link")
        try {
            client.linkEmail(currentSession, email.trim(), password.trim()).await()
            onSessionAuthenticated()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Failed to link Email account: ${e.message}", e)
            throw e
        }
    }

    private suspend fun onSessionAuthenticated() {
        session?.let { s ->
            prefs.edit().putString("nakama_jwt_token", s.authToken).apply()
            try {
                val account = client.getAccount(s).await()
                nakamaUserId = account.user.id
                nakamaUsername = account.user.username
                prefs.edit()
                    .putString("nakama_user_id", nakamaUserId)
                    .putString("nakama_username", nakamaUsername)
                    .apply()
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Session authenticated but account info fetch failed (likely network): ${e.message}")
            }
        }
    }

    suspend fun ensureAuthenticatedGuest(username: String): Boolean = withContext(Dispatchers.IO) {
        if (session != null && !session!!.IsExpired()) {
            return@withContext true
        }
        var deviceId = prefs.getString("nakama_device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
        }
        return@withContext authenticateWithDevice(deviceId, username)
    }

    fun getNakamaUserId(): String? = nakamaUserId

    // 2. Storage Sync (Stats, Coins, Rewards, Settings)
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

            // Also fetch account for the latest displayName and avatar_url
            try {
                val account = client.getAccount(s).await()
                if (!account.user.displayName.isNullOrBlank()) {
                    statsObj.put("displayName", account.user.displayName)
                }
                if (!account.user.username.isNullOrBlank()) {
                    statsObj.put("username", account.user.username)
                }
                if (account.user.avatarUrl != null) {
                    statsObj.put("avatarUrl", account.user.avatarUrl)
                }
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Could not fetch account info for avatar/displayName: ${e.message}")
            }

            return@withContext statsObj
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading user profile: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncUserProfileToNakama(profile: UserProfile) = withContext(Dispatchers.IO) {
        if (session == null || session?.IsExpired() == true) {
            val username = profile.displayName.ifBlank { "Duelist" }
            ensureAuthenticatedGuest(username)
        }
        val s = session ?: return@withContext
        try {
            val statsObj = JSONObject().apply {
                put("displayName", profile.displayName)
                put("username", nakamaUsername ?: profile.displayName)
                if (profile.photoUrl != null) {
                    put("avatarUrl", profile.photoUrl)
                }
                put("level", profile.level)
                put("xp", profile.xp)
                put("trophies", profile.trophies)
                put("wins", profile.wins)
                put("coins", profile.coins)
                put("rankTitle", profile.rankTitle)
                put("totalMatches", profile.totalMatches)
                put("wallsPlaced", profile.wallsPlaced)
                put("currentWinStreak", profile.currentWinStreak)
                put("longestWinStreak", profile.longestWinStreak)
            }

            val writeObj = StorageObjectWrite("user_data", "stats", statsObj.toString(), PermissionRead.PUBLIC_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            
            // Sync Account Profile (Display Name & Avatar)
            try {
                client.updateAccount(s, null, profile.displayName, profile.photoUrl, null, null, null).await()
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Failed to update Nakama account profile: ${e.message}")
            }

            // Post to leaderboard with avatarUrl in metadata (if leaderboard is initialized on server)
            try {
                val metadata = JSONObject().apply {
                    if (profile.photoUrl != null) {
                        put("avatarUrl", profile.photoUrl)
                    }
                }
                client.writeLeaderboardRecord(s, "global_rankings", profile.trophies.toLong(), profile.wins.toLong(), metadata.toString()).await()
            } catch (lbEx: Exception) {
                Log.d("NakamaRepository", "Leaderboard 'global_rankings' not yet created on server: ${lbEx.message}")
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error syncing user profile: ${e.message}")
        }
    }

    suspend fun syncUserSettingsToNakama(theme: BoardTheme) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val settingsObj = JSONObject().apply {
                put("boardTheme", theme.name)
            }
            val writeObj = StorageObjectWrite("user_data", "settings", settingsObj.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error syncing user settings: ${e.message}")
        }
    }

    suspend fun fetchUserSettingsFromNakama(): BoardTheme? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("settings")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val settings = JSONObject(result.getObjects(0).value)
                val themeName = settings.optString("boardTheme", BoardTheme.ELEGANT_DARK.name)
                return@withContext try { BoardTheme.valueOf(themeName) } catch(e: Exception) { BoardTheme.ELEGANT_DARK }
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading user settings: ${e.message}")
        }
        return@withContext null
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

    // 2b. Nakama Server Economy RPCs & Shop Synchronization
    suspend fun rpcVerifyAndProcessGooglePlayPurchase(
        productId: String,
        purchaseToken: String,
        orderId: String,
        amountCoins: Int
    ): Int = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext -1
        try {
            val payload = JSONObject().apply {
                put("productId", productId)
                put("purchaseToken", purchaseToken)
                put("orderId", orderId)
                put("amountCoins", amountCoins)
                put("userId", nakamaUserId)
            }
            try {
                val response = client.rpc(s, "verify_google_play_purchase", payload.toString()).await()
                if (!response.payload.isNullOrBlank()) {
                    val resObj = JSONObject(response.payload)
                    return@withContext resObj.optInt("new_balance", -1)
                }
            } catch (rpcEx: Exception) {
                Log.w("NakamaRepository", "RPC verify_google_play_purchase fallback: ${rpcEx.message}")
            }
            
            // Fallback to standard coin transaction RPC
            return@withContext rpcProcessCoinTransaction(amountCoins, "google_play_purchase:$productId:$orderId")
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Failed verifying Google Play purchase on Nakama: ${e.message}")
        }
        return@withContext -1
    }

    suspend fun rpcProcessCoinTransaction(amountChange: Int, reason: String): Int = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext -1
        try {
            val payload = JSONObject().apply {
                put("amount", amountChange)
                put("reason", reason)
                put("userId", nakamaUserId)
            }
            val response = client.rpc(s, "process_coin_transaction", payload.toString()).await()
            if (!response.payload.isNullOrBlank()) {
                val resObj = JSONObject(response.payload)
                return@withContext resObj.optInt("new_balance", -1)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "RPC process_coin_transaction notice: ${e.message}")
        }
        return@withContext -1
    }

    suspend fun fetchShopPacksFromNakama(): List<CoinPack>? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            // Try Nakama Server RPC first
            try {
                val response = client.rpc(s, "get_shop_packages", "{}").await()
                if (!response.payload.isNullOrBlank()) {
                    val array = JSONArray(response.payload)
                    val packs = mutableListOf<CoinPack>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        packs.add(
                            CoinPack(
                                id = obj.getString("id"),
                                nameEn = obj.getString("name"),
                                coins = obj.getInt("coins"),
                                priceUsd = obj.getString("price"),
                                popularTag = if (obj.has("popularTag")) obj.getString("popularTag") else null
                            )
                        )
                    }
                    if (packs.isNotEmpty()) return@withContext packs
                }
            } catch (e: Exception) {
                Log.w("NakamaRepository", "RPC get_shop_packages notice: ${e.message}")
            }

            // Fallback: Read Nakama Storage Object ("shop", "packages")
            val objectId = StorageObjectId("shop")
            objectId.setKey("packages")
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val array = JSONArray(result.getObjects(0).value)
                val packs = mutableListOf<CoinPack>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    packs.add(
                        CoinPack(
                            id = obj.getString("id"),
                            nameEn = obj.getString("name"),
                            coins = obj.getInt("coins"),
                            priceUsd = obj.getString("price"),
                            popularTag = if (obj.has("popularTag")) obj.getString("popularTag") else null
                        )
                    )
                }
                if (packs.isNotEmpty()) return@withContext packs
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error fetching shop packages from Nakama: ${e.message}")
        }
        return@withContext null
    }

    // 2c. Daily Streak, Daily Missions, & Daily Spinner Nakama Cloud Sync
    suspend fun syncDailyStreakToNakama(currentDay: Int, longestStreak: Int, lastClaimDate: String) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                put("currentDay", currentDay)
                put("longestStreak", longestStreak)
                put("lastClaimDate", lastClaimDate)
            }
            val writeObj = StorageObjectWrite("user_data", "daily_streak", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing daily streak to Nakama: ${e.message}")
        }
    }

    suspend fun fetchDailyStreakFromNakama(): JSONObject? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("daily_streak")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                return@withContext JSONObject(result.getObjects(0).value)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading daily streak from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncDailyMissionsToNakama(date: String, missionsArray: JSONArray) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                put("date", date)
                put("missions", missionsArray)
            }
            val writeObj = StorageObjectWrite("user_data", "daily_missions", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing daily missions to Nakama: ${e.message}")
        }
    }

    suspend fun fetchDailyMissionsFromNakama(): JSONObject? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("daily_missions")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                return@withContext JSONObject(result.getObjects(0).value)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading daily missions from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncEmojiSkinsToNakama(unlockedEmojiIds: Set<String>) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                val array = JSONArray()
                unlockedEmojiIds.forEach { array.put(it) }
                put("unlocked_emojis", array)
                put("updated_at", System.currentTimeMillis())
            }
            val writeObj = StorageObjectWrite("user_data", "unlocked_emojis", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            Log.d("NakamaRepository", "Successfully synced ${unlockedEmojiIds.size} emoji skins to server for user $nakamaUserId")
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing emoji skins to Nakama: ${e.message}")
        }
    }

    suspend fun fetchEmojiSkinsFromNakama(): Set<String>? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val effectiveUserId = nakamaUserId ?: s.userId
            val objectId = StorageObjectId("user_data")
            objectId.setKey("unlocked_emojis")
            if (!effectiveUserId.isNullOrBlank()) {
                objectId.setUserId(effectiveUserId)
            }
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val obj = JSONObject(result.getObjects(0).value)
                val array = obj.optJSONArray("unlocked_emojis")
                if (array != null) {
                    val set = mutableSetOf<String>()
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                    Log.d("NakamaRepository", "Successfully fetched ${set.size} emojis from server for user $effectiveUserId")
                    return@withContext set
                }
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading emoji skins from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncAvatarSkinsToNakama(unlockedSkinIds: Set<String>, selectedSkinId: String?) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                val array = JSONArray()
                unlockedSkinIds.forEach { array.put(it) }
                put("unlocked_skins", array)
                if (selectedSkinId != null) {
                    put("selected_skin_id", selectedSkinId)
                }
                put("updated_at", System.currentTimeMillis())
            }
            val writeObj = StorageObjectWrite("user_data", "avatar_skins", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            Log.d("NakamaRepository", "Successfully synced ${unlockedSkinIds.size} avatar skins to server (selected: $selectedSkinId)")
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing avatar skins to Nakama: ${e.message}")
        }
    }

    suspend fun fetchAvatarSkinsFromNakama(): Pair<Set<String>, String?>? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val effectiveUserId = nakamaUserId ?: s.userId
            val objectId = StorageObjectId("user_data")
            objectId.setKey("avatar_skins")
            if (!effectiveUserId.isNullOrBlank()) {
                objectId.setUserId(effectiveUserId)
            }
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val obj = JSONObject(result.getObjects(0).value)
                val array = obj.optJSONArray("unlocked_skins")
                val selected = obj.optString("selected_skin_id", "").ifBlank { null }
                val set = mutableSetOf<String>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                }
                Log.d("NakamaRepository", "Successfully fetched ${set.size} avatar skins from server (selected: $selected)")
                return@withContext Pair(set, selected)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading avatar skins from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncBallSkinsToNakama(unlockedBallIds: Set<String>, selectedBallId: String?) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                val array = JSONArray()
                unlockedBallIds.forEach { array.put(it) }
                put("unlocked_balls", array)
                if (selectedBallId != null) {
                    put("selected_ball_id", selectedBallId)
                }
                put("updated_at", System.currentTimeMillis())
            }
            val writeObj = StorageObjectWrite("user_data", "ball_skins", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            Log.d("NakamaRepository", "Successfully synced ${unlockedBallIds.size} ball skins to server (selected: $selectedBallId)")
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing ball skins to Nakama: ${e.message}")
        }
    }

    suspend fun fetchBallSkinsFromNakama(): Pair<Set<String>, String?>? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val effectiveUserId = nakamaUserId ?: s.userId
            val objectId = StorageObjectId("user_data")
            objectId.setKey("ball_skins")
            if (!effectiveUserId.isNullOrBlank()) {
                objectId.setUserId(effectiveUserId)
            }
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val obj = JSONObject(result.getObjects(0).value)
                val array = obj.optJSONArray("unlocked_balls")
                val selected = obj.optString("selected_ball_id", "").ifBlank { null }
                val set = mutableSetOf<String>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                }
                Log.d("NakamaRepository", "Successfully fetched ${set.size} ball skins from server (selected: $selected)")
                return@withContext Pair(set, selected)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading ball skins from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncWallSkinsToNakama(unlockedWallIds: Set<String>, selectedWallId: String?) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                val array = JSONArray()
                unlockedWallIds.forEach { array.put(it) }
                put("unlocked_walls", array)
                if (selectedWallId != null) {
                    put("selected_wall_id", selectedWallId)
                }
                put("updated_at", System.currentTimeMillis())
            }
            val writeObj = StorageObjectWrite("user_data", "wall_skins", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
            Log.d("NakamaRepository", "Successfully synced ${unlockedWallIds.size} wall skins to server (selected: $selectedWallId)")
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing wall skins to Nakama: ${e.message}")
        }
    }

    suspend fun fetchWallSkinsFromNakama(): Pair<Set<String>, String?>? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val effectiveUserId = nakamaUserId ?: s.userId
            val objectId = StorageObjectId("user_data")
            objectId.setKey("wall_skins")
            if (!effectiveUserId.isNullOrBlank()) {
                objectId.setUserId(effectiveUserId)
            }
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                val obj = JSONObject(result.getObjects(0).value)
                val array = obj.optJSONArray("unlocked_walls")
                val selected = obj.optString("selected_wall_id", "").ifBlank { null }
                val set = mutableSetOf<String>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                }
                Log.d("NakamaRepository", "Successfully fetched ${set.size} wall skins from server (selected: $selected)")
                return@withContext Pair(set, selected)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading wall skins from Nakama: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncDailySpinnerToNakama(lastSpinDate: String, totalSpins: Int, lastWonItem: String) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val json = JSONObject().apply {
                put("lastSpinDate", lastSpinDate)
                put("totalSpins", totalSpins)
                put("lastWonItem", lastWonItem)
            }
            val writeObj = StorageObjectWrite("user_data", "daily_spinner", json.toString(), PermissionRead.OWNER_READ, PermissionWrite.OWNER_WRITE)
            client.writeStorageObjects(s, writeObj).await()
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error syncing daily spinner to Nakama: ${e.message}")
        }
    }

    suspend fun fetchDailySpinnerFromNakama(): JSONObject? = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext null
        try {
            val objectId = StorageObjectId("user_data")
            objectId.setKey("daily_spinner")
            objectId.setUserId(nakamaUserId)
            val result = client.readStorageObjects(s, objectId).await()
            if (result.objectsCount > 0) {
                return@withContext JSONObject(result.getObjects(0).value)
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error reading daily spinner from Nakama: ${e.message}")
        }
        return@withContext null
    }

    // 3. Leaderboards
    suspend fun fetchGlobalLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        // Ensure active session
        if (session == null || session?.IsExpired() == true) {
            val username = nakamaUsername ?: prefs.getString("nakama_username", null) ?: "player_${java.util.UUID.randomUUID().toString().take(8)}"
            ensureAuthenticatedGuest(username)
        }
        val s = session ?: return@withContext emptyList()

        var entries: List<LeaderboardEntry> = emptyList()

        // 1. First attempt to query Nakama global_rankings leaderboard
        try {
            val result = client.listLeaderboardRecords(s, "global_rankings").await()
            if (result.recordsList.isNotEmpty()) {
                val ownerIds = result.recordsList.map { it.ownerId }.filter { it.isNotBlank() }.distinct()
                val userMap = mutableMapOf<String, com.heroiclabs.nakama.api.User>()
                if (ownerIds.isNotEmpty()) {
                    try {
                        val usersResult = client.getUsers(s, ownerIds).await()
                        usersResult.usersList?.forEach { u ->
                            userMap[u.id] = u
                        }
                    } catch (e: Exception) {
                        Log.w("NakamaRepository", "Failed to fetch user profiles for leaderboard: ${e.message}")
                    }
                }

                entries = result.recordsList.mapIndexed { index, rec ->
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
                    val userObj = userMap[rec.ownerId]
                    if (avatarUrl.isNullOrBlank() && userObj != null && !userObj.avatarUrl.isNullOrBlank()) {
                        avatarUrl = userObj.avatarUrl
                    }
                    val resolvedDisplayName = when {
                        userObj != null && !userObj.displayName.isNullOrBlank() -> userObj.displayName
                        rec.hasUsername() && rec.username.value.isNotBlank() -> rec.username.value
                        else -> "Duelist"
                    }

                    LeaderboardEntry(
                        rank = rec.rank.toInt().takeIf { it != 0 } ?: (index + 1),
                        userId = rec.ownerId,
                        username = if (rec.hasUsername()) rec.username.value else "Duelist",
                        displayName = resolvedDisplayName,
                        trophies = rec.score.toInt(),
                        wins = rec.subscore.toInt(),
                        level = (rec.score.toInt() / 200) + 1,
                        avatarUrl = avatarUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Leaderboard 'global_rankings' query failed or not found: ${e.message}")
        }

        // 2. Fallback: If leaderboard has no records or is not created on server, list all users from public stats storage
        if (entries.isEmpty()) {
            try {
                // Fetch all user storage objects with pagination support using listUsersStorageObjects
                val allStorageObjects = mutableListOf<com.heroiclabs.nakama.api.StorageObject>()
                var cursor: String? = null
                var page = 0
                do {
                    page++
                    val storageResult = if (cursor.isNullOrBlank()) {
                        client.listUsersStorageObjects(s, "user_data", null, 100).await()
                    } else {
                        client.listUsersStorageObjects(s, "user_data", null, 100, cursor).await()
                    }
                    allStorageObjects.addAll(storageResult.objectsList)
                    cursor = storageResult.cursor?.takeIf { it.isNotBlank() }
                    Log.d("NakamaRepository", "Storage fetch page $page: got ${storageResult.objectsList.size} objects (total: ${allStorageObjects.size}), next cursor=${cursor?.take(10)}")
                } while (!cursor.isNullOrBlank() && page < 25)

                val statsObjects = allStorageObjects.filter { it.key == "stats" }
                Log.d("NakamaRepository", "Filtered ${statsObjects.size} 'stats' objects from ${allStorageObjects.size} total storage objects")
                if (statsObjects.isNotEmpty()) {
                    // Deduplicate by userId
                    val uniqueStats = statsObjects.groupBy { it.userId }.map { (_, list) -> list.last() }
                    val userIds = uniqueStats.map { it.userId }.filter { it.isNotBlank() }.distinct()
                    val userMap = mutableMapOf<String, com.heroiclabs.nakama.api.User>()
                    if (userIds.isNotEmpty()) {
                        // Chunk userIds in batches of 50 to prevent gRPC/HTTP payload size limits
                        for (batch in userIds.chunked(50)) {
                            try {
                                val usersResult = client.getUsers(s, batch).await()
                                usersResult.usersList?.forEach { u ->
                                    userMap[u.id] = u
                                }
                            } catch (e: Exception) {
                                Log.w("NakamaRepository", "Failed to fetch user batch for storage objects: ${e.message}")
                            }
                        }
                    }

                    val storageEntries = uniqueStats.mapNotNull { obj ->
                        try {
                            val json = JSONObject(obj.value)
                            val trophies = json.optInt("trophies", 0)
                            val wins = json.optInt("wins", 0)
                            val level = json.optInt("level", (trophies / 200) + 1)
                            val u = userMap[obj.userId]
                            val jsonDisplayName = json.optString("displayName", "").takeIf { it.isNotBlank() }
                            val jsonUsername = json.optString("username", "").takeIf { it.isNotBlank() }
                            val jsonAvatarUrl = json.optString("avatarUrl", "").takeIf { it.isNotBlank() }
                            val name = when {
                                !jsonDisplayName.isNullOrBlank() -> jsonDisplayName
                                u != null && !u.displayName.isNullOrBlank() -> u.displayName
                                u != null && !u.username.isNullOrBlank() -> u.username
                                !jsonUsername.isNullOrBlank() -> jsonUsername
                                else -> null
                            } ?: return@mapNotNull null

                            // Filter out guest accounts from leaderboard rankings
                            val cleanName = name.trim()
                            if (cleanName.startsWith("Guest_", ignoreCase = true) ||
                                cleanName.equals("Guest", ignoreCase = true) ||
                                cleanName.startsWith("Guest Duelist", ignoreCase = true) ||
                                cleanName.startsWith("Duelist", ignoreCase = true) ||
                                cleanName.startsWith("player_", ignoreCase = true)
                            ) {
                                return@mapNotNull null
                            }

                            val avatar = u?.avatarUrl?.takeIf { it.isNotBlank() } ?: jsonAvatarUrl
                            LeaderboardEntry(
                                rank = 0,
                                userId = obj.userId,
                                username = u?.username ?: jsonUsername ?: name,
                                displayName = name,
                                trophies = trophies,
                                wins = wins,
                                level = level,
                                avatarUrl = avatar
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Sort by trophies desc, then wins desc
                    entries = storageEntries
                        .sortedWith(
                            compareByDescending<LeaderboardEntry> { it.trophies }
                                .thenByDescending { it.wins }
                        )
                        .mapIndexed { idx, item ->
                            item.copy(rank = idx + 1)
                        }
                }
            } catch (e: Exception) {
                Log.w("NakamaRepository", "Failed to list storage objects for leaderboard: ${e.message}")
            }
        }

        _leaderboard.value = entries
        entries
    }

    // 4. Friends
    suspend fun fetchFriends(): List<NakamaFriend> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext _friends.value
        try {
            val result = client.listFriends(s).await()
            val list = result.friendsList.map { f ->
                NakamaFriend(
                    userId = f.user.id,
                    username = f.user.username,
                    displayName = if (!f.user.displayName.isNullOrBlank()) f.user.displayName else f.user.username,
                    isOnline = f.user.online,
                    level = 1,
                    trophies = 0,
                    avatarUrl = f.user.avatarUrl,
                    state = f.state.value
                )
            }
            _friends.value = list
            list
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error fetching friends: ${e.message}")
            _friends.value
        }
    }

    suspend fun addFriendByUsername(targetIdentifier: String): Boolean = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext false
        val cleanTarget = targetIdentifier.trim()
        if (cleanTarget.isBlank()) return@withContext false
        
        try {
            // First attempt: Add by username directly
            client.addFriends(s, emptyList<String>(), cleanTarget).await()
            fetchFriends()
            true
        } catch (e: Exception) {
            Log.w("NakamaRepository", "addFriends with username failed: ${e.message}, trying lookup or user ID...")
            try {
                // Second attempt: Maybe the input is a userId or needs lookup
                val usersResult = try {
                    client.getUsers(s, emptyList<String>(), cleanTarget).await()
                } catch (lookupEx: Exception) {
                    null
                }
                
                val foundUser = usersResult?.usersList?.firstOrNull()
                if (foundUser != null) {
                    client.addFriends(s, listOf(foundUser.id)).await()
                    fetchFriends()
                    true
                } else {
                    // Try passing as user ID directly
                    client.addFriends(s, listOf(cleanTarget)).await()
                    fetchFriends()
                    true
                }
            } catch (fallbackEx: Exception) {
                Log.e("NakamaRepository", "All attempts to add friend '$cleanTarget' failed: ${fallbackEx.message}")
                false
            }
        }
    }

    suspend fun removeFriend(username: String): Boolean = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext false
        try {
            val friend = _friends.value.find { it.username.equals(username, ignoreCase = true) || it.userId == username }
            if (friend != null) {
                client.deleteFriends(s, listOf(friend.userId)).await()
            } else {
                client.deleteFriends(s, emptyList<String>(), username).await()
            }
            fetchFriends()
            true
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error removing friend: ${e.message}")
            false
        }
    }

    // 5. Matchmaking & Real-time Sockets
    fun startOnlineMatchmaking(username: String, arenaId: String = "pro") {
        scope.launch {
            _matchState.value = OnlineMatchState.CONNECTING
            _matchEvents.emit(OnlineMatchEvent.SearchingForMatch)

            if (session == null || session!!.IsExpired()) {
                if (!ensureAuthenticatedGuest(username)) {
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

                        override fun onMatchPresence(matchPresence: MatchPresenceEvent) {
                            scope.launch {
                                val leaves = matchPresence.leaves
                                val joins = matchPresence.joins
                                if (leaves != null && leaves.any { it.userId != nakamaUserId }) {
                                    _matchEvents.emit(OnlineMatchEvent.OpponentDisconnected)
                                }
                                if (joins != null && joins.any { it.userId != nakamaUserId }) {
                                    _matchEvents.emit(OnlineMatchEvent.OpponentReconnected)
                                }
                            }
                        }

                        override fun onDisconnect(t: Throwable?) {
                            Log.w("NakamaRepository", "Socket disconnected: ${t?.message}")
                            socket = null
                            if (activeMatchId != null) {
                                _matchState.value = OnlineMatchState.DISCONNECTED
                            } else {
                                _matchState.value = OnlineMatchState.IDLE
                            }
                        }
                    })?.await()
                }

                _matchState.value = OnlineMatchState.SEARCHING_MATCH
                val query = "+properties.arena_id:$arenaId"
                val stringProps = mapOf("arena_id" to arenaId)
                NakamaBridge.addMatchmaker(socket!!, 2, 2, query, stringProps, null, 1).await()
            } catch (e: Exception) {
                Log.e("NakamaRepository", "Socket connection error: ${e.message}")
                _matchState.value = OnlineMatchState.ERROR
                _matchEvents.emit(OnlineMatchEvent.Error("Unable to connect to game servers. Please check your internet connection and try again."))
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
                var opponentUserId: String? = null
                
                // Deterministically sort matched users by presence userId so both clients agree on player 0 and player 1
                val sortedUsers = matched.users.sortedBy { it.presence.userId }
                val myUserId = session?.userId ?: nakamaUserId ?: matched.self?.presence?.userId
                
                sortedUsers.forEachIndexed { index, user ->
                    if (user.presence.userId == myUserId || 
                        (nakamaUserId != null && user.presence.userId == nakamaUserId) ||
                        (nakamaUsername != null && user.presence.username == nakamaUsername)
                    ) {
                        selfIndex = index
                    } else {
                        opponentUserId = user.presence.userId
                        oppName = user.presence.username ?: "Online Opponent"
                    }
                }

                // Try to fetch real display name for opponent
                opponentUserId?.let { oppId ->
                    try {
                        session?.let { s ->
                            val usersResult = client.getUsers(s, oppId).await()
                            val userList = usersResult.usersList
                            if (userList != null && userList.isNotEmpty()) {
                                val u = userList[0]
                                if (!u.displayName.isNullOrBlank()) {
                                    oppName = u.displayName
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("NakamaRepository", "Could not fetch opponent real name: ${e.message}")
                    }
                }

                myPlayerIndex = selfIndex
                val starterIndex = 0
                _matchState.value = OnlineMatchState.IN_MATCH
                _matchEvents.emit(OnlineMatchEvent.MatchFound(activeMatchId!!, myPlayerIndex, oppName, starterIndex))
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
                5 -> { // OP_EMOTE
                    val emojiId = payloadJson.optString("emojiId", "emoji_cool")
                    val emojiSymbol = payloadJson.optString("emojiSymbol", "😎")
                    val playerIndex = payloadJson.optInt("playerIndex", if (myPlayerIndex == 0) 1 else 0)
                    _matchEvents.emit(OnlineMatchEvent.OpponentEmote(emojiId, emojiSymbol, playerIndex))
                }
            }
        }
    }

    fun sendOnlineEmote(emojiId: String, emojiSymbol: String) {
        val mid = activeMatchId ?: return
        val sock = socket ?: return

        try {
            val payload = JSONObject().apply {
                put("emojiId", emojiId)
                put("emojiSymbol", emojiSymbol)
                put("playerIndex", myPlayerIndex)
            }
            sock.sendMatchData(mid, 5L, payload.toString().toByteArray())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending emote: ${e.message}")
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

    fun sendTurnTimeout() {
        val mid = activeMatchId ?: return
        val sock = socket ?: return

        try {
            val payload = JSONObject().apply {
                put("playerIndex", myPlayerIndex)
            }
            sock.sendMatchData(mid, 3L, payload.toString().toByteArray())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending turn timeout: ${e.message}")
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

    fun attemptReconnectActiveMatch() {
        val matchId = activeMatchId ?: return
        scope.launch {
            try {
                if (session == null || session!!.IsExpired()) {
                    val username = nakamaUsername ?: "Player"
                    ensureAuthenticatedGuest(username)
                }
                if (session != null) {
                    if (socket == null) {
                        socket = client.createSocket()
                        socket?.connect(session!!, object : AbstractSocketListener() {
                            override fun onMatchmakerMatched(matched: MatchmakerMatched) {
                                handleMatchmakerMatched(matched)
                            }

                            override fun onMatchData(matchData: MatchData) {
                                handleIncomingMatchData(matchData)
                            }

                            override fun onMatchPresence(matchPresence: MatchPresenceEvent) {
                                scope.launch {
                                    val leaves = matchPresence.leaves
                                    val joins = matchPresence.joins
                                    if (leaves != null && leaves.any { it.userId != nakamaUserId }) {
                                        _matchEvents.emit(OnlineMatchEvent.OpponentDisconnected)
                                    }
                                    if (joins != null && joins.any { it.userId != nakamaUserId }) {
                                        _matchEvents.emit(OnlineMatchEvent.OpponentReconnected)
                                    }
                                }
                            }

                            override fun onDisconnect(t: Throwable?) {
                                Log.w("NakamaRepository", "Socket disconnected: ${t?.message}")
                                socket = null
                                if (activeMatchId != null) {
                                    _matchState.value = OnlineMatchState.DISCONNECTED
                                } else {
                                    _matchState.value = OnlineMatchState.IDLE
                                }
                            }
                        })?.await()
                    }

                    if (socket != null && activeMatchId != null) {
                        socket?.joinMatch(matchId)?.await()
                        _matchState.value = OnlineMatchState.IN_MATCH
                        Log.d("NakamaRepository", "Successfully reconnected and rejoined match: $matchId")
                    }
                }
            } catch (e: Exception) {
                Log.e("NakamaRepository", "Reconnect attempt failed: ${e.message}")
            }
        }
    }
}
