package com.example.data.nakama

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.data.MatchRecord
import com.example.data.UserProfile
import com.example.model.Move
import com.example.model.Position
import com.example.model.Wall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class OnlineMatchEvent {
    object SearchingForMatch : OnlineMatchEvent()
    data class MatchFound(val matchId: String, val selfPlayerIndex: Int, val opponentName: String) : OnlineMatchEvent()
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

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // Config
    private val _config = MutableStateFlow(
        NakamaConfig(
            host = prefs.getString("nakama_host", "10.0.2.2") ?: "10.0.2.2",
            port = prefs.getInt("nakama_port", 7350),
            serverKey = prefs.getString("nakama_server_key", "defaultkey") ?: "defaultkey",
            useSsl = prefs.getBoolean("nakama_ssl", false)
        )
    )
    val config: StateFlow<NakamaConfig> = _config.asStateFlow()

    // Session
    private var jwtSessionToken: String? = prefs.getString("nakama_jwt_token", null)
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
    private var webSocket: WebSocket? = null
    private var activeMatchId: String? = null
    private var myPlayerIndex: Int = 0 // 0 = Bottom (Host), 1 = Top (Joiner)
    private var currentTurnPlayer: Int = 0

    fun updateConfig(host: String, port: Int, serverKey: String, useSsl: Boolean) {
        prefs.edit()
            .putString("nakama_host", host.trim())
            .putInt("nakama_port", port)
            .putString("nakama_server_key", serverKey.trim())
            .putBoolean("nakama_ssl", useSsl)
            .apply()

        _config.value = NakamaConfig(
            host = host.trim(),
            port = port,
            serverKey = serverKey.trim(),
            useSsl = useSsl
        )
    }

    private fun getBasicAuthHeader(): String {
        val credentials = "${config.value.serverKey}:"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    private fun getBearerAuthHeader(): String {
        return "Bearer ${jwtSessionToken ?: ""}"
    }

    // 1. Google or Custom Authentication with Nakama
    suspend fun authenticateWithGoogle(googleIdToken: String, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${config.value.httpBaseUrl}/v2/account/authenticate/google?create=true&username=${username.filter { it.isLetterOrDigit() }}"
            val json = JSONObject().apply {
                put("token", googleIdToken)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBasicAuthHeader())
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response: Response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val jsonObj = JSONObject(respBody)
                jwtSessionToken = jsonObj.getString("token")

                // Fetch Account Details
                fetchAccountDetails()
                saveSession()
                true
            } else {
                Log.w("NakamaRepository", "Google Auth failed on server with code ${response.code}, falling back to Custom Auth")
                authenticateWithDeviceInternal(username)
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error in authenticateWithGoogle: ${e.message}")
            authenticateWithDeviceInternal(username)
        }
    }

    suspend fun authenticateWithEmail(
        email: String,
        password: String,
        username: String = "",
        isSignUp: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val sanitizedUsername = username.filter { it.isLetterOrDigit() }.ifBlank { "Player${(1000..9999).random()}" }
            val url = if (isSignUp) {
                "${config.value.httpBaseUrl}/v2/account/authenticate/email?create=true&username=$sanitizedUsername"
            } else {
                "${config.value.httpBaseUrl}/v2/account/authenticate/email?create=false"
            }

            val json = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBasicAuthHeader())
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response: Response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val jsonObj = JSONObject(respBody)
                jwtSessionToken = jsonObj.getString("token")

                fetchAccountDetailsInternal()
                saveSession()
                Log.i("NakamaRepository", "Successfully authenticated email with Nakama!")
                Result.success(true)
            } else {
                val errBody = response.body?.string() ?: ""
                Log.e("NakamaRepository", "Email Auth failed with code ${response.code}: $errBody")
                Result.failure(Exception("Email authentication failed (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Email Auth error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun authenticateWithDevice(username: String): Boolean = withContext(Dispatchers.IO) {
        authenticateWithDeviceInternal(username)
    }

    private fun authenticateWithDeviceInternal(username: String): Boolean {
        return try {
            var deviceId = prefs.getString("nakama_device_id", null)
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString()
                prefs.edit().putString("nakama_device_id", deviceId).apply()
            }

            val sanitizedUsername = username.filter { it.isLetterOrDigit() }.ifBlank { "Player${(1000..9999).random()}" }
            
            // Try device authentication endpoint first
            val urlDevice = "${config.value.httpBaseUrl}/v2/account/authenticate/device?create=true&username=$sanitizedUsername"
            val jsonDevice = JSONObject().apply {
                put("id", deviceId)
            }

            val requestDevice = Request.Builder()
                .url(urlDevice)
                .addHeader("Authorization", getBasicAuthHeader())
                .post(jsonDevice.toString().toRequestBody(jsonMediaType))
                .build()

            var response: Response = httpClient.newCall(requestDevice).execute()
            if (!response.isSuccessful) {
                // Try custom auth endpoint as fallback
                val urlCustom = "${config.value.httpBaseUrl}/v2/account/authenticate/custom?create=true&username=$sanitizedUsername"
                val requestCustom = Request.Builder()
                    .url(urlCustom)
                    .addHeader("Authorization", getBasicAuthHeader())
                    .post(jsonDevice.toString().toRequestBody(jsonMediaType))
                    .build()
                response = httpClient.newCall(requestCustom).execute()
            }

            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val jsonObj = JSONObject(respBody)
                jwtSessionToken = jsonObj.getString("token")

                fetchAccountDetailsInternal()
                saveSession()
                Log.i("NakamaRepository", "Successfully authenticated device with Nakama!")
                true
            } else {
                Log.e("NakamaRepository", "Device & Custom Auth failed with code ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Device auth error: ${e.message}")
            false
        }
    }

    private suspend fun fetchAccountDetails() = withContext(Dispatchers.IO) {
        fetchAccountDetailsInternal()
    }

    private fun fetchAccountDetailsInternal() {
        if (jwtSessionToken == null) return
        try {
            val url = "${config.value.httpBaseUrl}/v2/account"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val json = JSONObject(respBody)
                val user = json.getJSONObject("user")
                nakamaUserId = user.getString("id")
                nakamaUsername = user.optString("username", "Player")
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Failed to fetch Nakama account details: ${e.message}")
        }
    }

    private fun saveSession() {
        prefs.edit()
            .putString("nakama_jwt_token", jwtSessionToken)
            .putString("nakama_user_id", nakamaUserId)
            .putString("nakama_username", nakamaUsername)
            .apply()
    }

    suspend fun ensureAuthenticatedGuest(): Boolean = withContext(Dispatchers.IO) {
        if (!jwtSessionToken.isNull_or_blank() && nakamaUserId != null) {
            return@withContext true
        }
        val defaultGuestName = "Guest_${(1000..9999).random()}"
        return@withContext authenticateWithDeviceInternal(defaultGuestName)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

    fun getNakamaUserId(): String? = nakamaUserId

    // 2. Sync User Stats & Coins (+75 coins per win!)
    suspend fun fetchUserProfileFromNakama(): JSONObject? = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) return@withContext null
        try {
            val url = "${config.value.httpBaseUrl}/v2/storage/read"
            val readPayload = JSONObject().apply {
                put("object_ids", JSONArray().put(JSONObject().apply {
                    put("collection", "user_data")
                    put("key", "stats")
                    put("user_id", nakamaUserId)
                }))
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post(readPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val objects = json.optJSONArray("objects")
                if (objects != null && objects.length() > 0) {
                    val valStr = objects.getJSONObject(0).optString("value", "{}")
                    return@withContext JSONObject(valStr)
                }
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading user profile from Nakama storage: ${e.message}")
        }
        return@withContext null
    }

    suspend fun syncUserProfileToNakama(profile: UserProfile) = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) {
            ensureAuthenticatedGuest()
        }
        if (jwtSessionToken == null) return@withContext
        try {
            // Write to Storage
            val url = "${config.value.httpBaseUrl}/v2/storage"
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

            val objectObj = JSONObject().apply {
                put("collection", "user_data")
                put("key", "stats")
                put("value", statsObj.toString())
                put("permission_read", 2) // Public read
                put("permission_write", 1) // Owner write
            }

            val payload = JSONObject().apply {
                put("objects", JSONArray().put(objectObj))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute()

            // Write Leaderboard record
            postLeaderboardRecordInternal(profile.trophies, profile.wins)
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error syncing user profile to Nakama: ${e.message}")
        }
    }

    suspend fun recordMatchHistoryToNakama(matchRecord: MatchRecord) = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) {
            ensureAuthenticatedGuest()
        }
        if (jwtSessionToken == null) return@withContext
        try {
            val existingHistory = fetchMatchHistoryFromNakama().toMutableList()
            existingHistory.add(0, matchRecord)
            val historyLimit = existingHistory.take(20)

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

            val url = "${config.value.httpBaseUrl}/v2/storage"
            val objectObj = JSONObject().apply {
                put("collection", "user_data")
                put("key", "match_history")
                put("value", arrayJson.toString())
                put("permission_read", 2)
                put("permission_write", 1)
            }
            val payload = JSONObject().apply {
                put("objects", JSONArray().put(objectObj))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error recording match history to Nakama: ${e.message}")
        }
    }

    suspend fun fetchMatchHistoryFromNakama(): List<MatchRecord> = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) return@withContext emptyList()
        try {
            val url = "${config.value.httpBaseUrl}/v2/storage/read"
            val readPayload = JSONObject().apply {
                put("object_ids", JSONArray().put(JSONObject().apply {
                    put("collection", "user_data")
                    put("key", "match_history")
                    put("user_id", nakamaUserId)
                }))
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post(readPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val objects = json.optJSONArray("objects")
                if (objects != null && objects.length() > 0) {
                    val valStr = objects.getJSONObject(0).optString("value", "[]")
                    val array = JSONArray(valStr)
                    val list = mutableListOf<MatchRecord>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            MatchRecord(
                                id = i.toLong(),
                                modeName = obj.optString("modeName", "Classic Duel"),
                                opponentName = obj.optString("opponentName", "Opponent"),
                                winnerPlayer = obj.optInt("winnerPlayer", 0),
                                totalMoves = obj.optInt("totalMoves", 0),
                                totalWallsPlaced = obj.optInt("totalWallsPlaced", 0),
                                durationSeconds = obj.optLong("durationSeconds", 0L),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error reading match history from Nakama storage: ${e.message}")
        }
        return@withContext emptyList()
    }

    // 3. Ranking Chart / Leaderboards
    suspend fun fetchGlobalLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) return@withContext getMockLeaderboard()
        try {
            val url = "${config.value.httpBaseUrl}/v2/leaderboard/global_rankings/records?limit=20"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val records = json.optJSONArray("records")
                val list = mutableListOf<LeaderboardEntry>()

                if (records != null) {
                    for (i in 0 until records.length()) {
                        val rec = records.getJSONObject(i)
                        val rank = rec.optInt("rank", i + 1)
                        val userId = rec.optString("owner_id", "")
                        val username = rec.optString("username", "Duelist #$rank")
                        val score = rec.optInt("score", 0)
                        val subscore = rec.optInt("subscore", 0)

                        list.add(
                            LeaderboardEntry(
                                rank = rank,
                                userId = userId,
                                username = username,
                                displayName = username,
                                trophies = score,
                                wins = subscore,
                                level = (score / 200) + 1
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _leaderboard.value = list
                    list
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("NakamaRepository", "Error fetching Nakama leaderboard: ${e.message}")
            emptyList()
        }
    }

    private fun postLeaderboardRecordInternal(trophies: Int, wins: Int) {
        if (jwtSessionToken == null) return
        try {
            val url = "${config.value.httpBaseUrl}/v2/leaderboard/global_rankings"
            val body = JSONObject().apply {
                put("score", trophies.toString())
                put("subscore", wins.toString())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Failed posting score to Nakama leaderboard: ${e.message}")
        }
    }

    private fun getMockLeaderboard(): List<LeaderboardEntry> {
        return emptyList()
    }

    // 4. Friends Search & Management
    suspend fun fetchFriends(): List<NakamaFriend> = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) return@withContext _friends.value
        try {
            val url = "${config.value.httpBaseUrl}/v2/friend"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val friendsArray = json.optJSONArray("friends")
                val list = mutableListOf<NakamaFriend>()

                if (friendsArray != null) {
                    for (i in 0 until friendsArray.length()) {
                        val item = friendsArray.getJSONObject(i)
                        val user = item.getJSONObject("user")
                        val state = item.optInt("state", 0) // 0 = Mutual Friends
                        if (state == 0) {
                            list.add(
                                NakamaFriend(
                                    userId = user.getString("id"),
                                    username = user.optString("username", "Friend"),
                                    displayName = user.optString("display_name", user.optString("username", "Friend")),
                                    isOnline = user.optBoolean("online", true),
                                    level = (1..5).random(),
                                    trophies = (100..800).random()
                                )
                            )
                        }
                    }
                }
                _friends.value = list
                list
            } else {
                _friends.value
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error fetching friends: ${e.message}")
            _friends.value
        }
    }

    suspend fun addFriendByUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        if (jwtSessionToken == null) {
            // Local fallback for offline/guest mode friend simulation
            val current = _friends.value.toMutableList()
            if (current.none { it.username.equals(username, ignoreCase = true) }) {
                current.add(
                    NakamaFriend(
                        userId = UUID.randomUUID().toString(),
                        username = username,
                        displayName = username,
                        isOnline = true,
                        level = (1..4).random(),
                        trophies = (150..600).random()
                    )
                )
                _friends.value = current
            }
            return@withContext true
        }

        try {
            val url = "${config.value.httpBaseUrl}/v2/friend?usernames=${username.trim()}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", getBearerAuthHeader())
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                fetchFriends()
                true
            } else {
                // Fallback local list addition
                val current = _friends.value.toMutableList()
                current.add(
                    NakamaFriend(
                        userId = UUID.randomUUID().toString(),
                        username = username,
                        displayName = username,
                        isOnline = true
                    )
                )
                _friends.value = current
                true
            }
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error adding friend: ${e.message}")
            false
        }
    }

    suspend fun removeFriend(username: String): Boolean {
        _friends.value = _friends.value.filterNot { it.username.equals(username, ignoreCase = true) }
        return true
    }

    // 5. Realtime Online Game Matchmaking & WebSocket Sync
    fun startOnlineMatchmaking(username: String) {
        scope.launch {
            _matchState.value = OnlineMatchState.CONNECTING
            _matchEvents.emit(OnlineMatchEvent.SearchingForMatch)

            // Always ensure we have a valid, active session token before connecting WebSocket
            if (jwtSessionToken.isNullOrBlank()) {
                val success = authenticateWithDevice(username)
                if (!success) {
                    _matchState.value = OnlineMatchState.ERROR
                    Log.e("NakamaRepository", "Authentication with Nakama server failed")
                    _matchEvents.emit(
                        OnlineMatchEvent.Error("Authentication failed on Nakama server (${config.value.host}:${config.value.effectivePort}). Ensure Nakama is running on port 7350.")
                    )
                    return@launch
                }
            }

            connectWebSocketAndQueueMatchmaker()
        }
    }

    fun challengeFriendToMatch(friendUsername: String) {
        scope.launch {
            _matchState.value = OnlineMatchState.CONNECTING
            _matchEvents.emit(OnlineMatchEvent.SearchingForMatch)

            if (jwtSessionToken.isNullOrBlank()) {
                val success = authenticateWithDevice("Player")
                if (!success) {
                    _matchState.value = OnlineMatchState.ERROR
                    _matchEvents.emit(
                        OnlineMatchEvent.Error("Authentication failed on Nakama server (${config.value.host}:${config.value.effectivePort}).")
                    )
                    return@launch
                }
            }

            connectWebSocketAndQueueMatchmaker(friendFilter = friendUsername)
        }
    }

    private fun connectWebSocketAndQueueMatchmaker(friendFilter: String? = null, isRetry: Boolean = false, useWsPath: Boolean = false) {
        try {
            val token = jwtSessionToken
            if (token.isNullOrBlank()) {
                _matchState.value = OnlineMatchState.ERROR
                scope.launch {
                    _matchEvents.emit(OnlineMatchEvent.Error("No valid Nakama JWT session token found. Please re-authenticate."))
                }
                return
            }

            val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
            val path = if (useWsPath) "/ws" else "/v2/socket"
            val wsUrl = "${config.value.wsBaseUrl}$path?token=$encodedToken&format=json"
            val originHeader = config.value.httpBaseUrl

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", getBearerAuthHeader())
                .addHeader("Origin", originHeader)
                .build()

            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.i("NakamaRepository", "WebSocket connected successfully to Nakama endpoint: $path")
                    _matchState.value = OnlineMatchState.SEARCHING_MATCH

                    // Send Matchmaker Add request to Nakama Server
                    val cid = UUID.randomUUID().toString()
                    val msg = JSONObject().apply {
                        put("cid", cid)
                        put("matchmaker_add", JSONObject().apply {
                            put("min_count", 2)
                            put("max_count", 2)
                            put("query", if (friendFilter != null) "properties.friend:'$friendFilter'" else "*")
                        })
                    }
                    ws.send(msg.toString())
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleIncomingWsMessage(text)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e("NakamaRepository", "Nakama WebSocket failed on path $path: ${t.message}, HTTP code: ${response?.code}")

                    // If /v2/socket gave 404, try /ws path as fallback before erroring out
                    if (!useWsPath && response?.code == 404) {
                        Log.w("NakamaRepository", "/v2/socket returned 404, trying /ws endpoint fallback...")
                        connectWebSocketAndQueueMatchmaker(friendFilter, isRetry = isRetry, useWsPath = true)
                        return
                    }

                    // Handle HTTP 404/401 Token Expiry/Invalidation automatically
                    if (!isRetry && (response?.code == 404 || response?.code == 401 || t is java.net.ProtocolException)) {
                        Log.w("NakamaRepository", "WebSocket token rejected (HTTP ${response?.code}). Re-authenticating with device...")
                        jwtSessionToken = null
                        prefs.edit().remove("nakama_jwt_token").apply()

                        scope.launch {
                            val reauthSuccess = authenticateWithDeviceInternal("Player")
                            if (reauthSuccess) {
                                connectWebSocketAndQueueMatchmaker(friendFilter, isRetry = true)
                            } else {
                                _matchState.value = OnlineMatchState.ERROR
                                _matchEvents.emit(
                                    OnlineMatchEvent.Error(
                                        "Nakama WebSocket authentication failed (HTTP ${response?.code ?: 404}). Ensure Nakama is running on port 7350."
                                    )
                                )
                            }
                        }
                        return
                    }

                    _matchState.value = OnlineMatchState.ERROR
                    scope.launch {
                        _matchEvents.emit(
                            OnlineMatchEvent.Error(
                                "Server Connection Failed (${config.value.host}:${config.value.effectivePort}). Ensure Nakama is running on port 7350."
                            )
                        )
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _matchState.value = OnlineMatchState.IDLE
                }
            })
        } catch (e: Exception) {
            Log.e("NakamaRepository", "WebSocket connection error: ${e.message}")
            _matchState.value = OnlineMatchState.ERROR
            scope.launch {
                _matchEvents.emit(
                    OnlineMatchEvent.Error("Nakama Connection Error: ${e.message}")
                )
            }
        }
    }

    private fun handleIncomingWsMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("matchmaker_matched")) {
                val matched = json.getJSONObject("matchmaker_matched")
                val token = matched.optString("token", "")
                val matchId = matched.optString("match_id", "")

                val users = matched.optJSONArray("users") ?: JSONArray()
                var oppName = "Online Opponent"
                var selfIndex = 0

                for (i in 0 until users.length()) {
                    val u = users.getJSONObject(i)
                    val p = u.optJSONObject("presence")
                    val uId = p?.optString("user_id", "")
                    if (uId == nakamaUserId) {
                        selfIndex = i
                    } else if (p != null) {
                        oppName = p.optString("username", "Online Opponent")
                    }
                }

                myPlayerIndex = selfIndex
                currentTurnPlayer = 0

                // Join Nakama Match via Token/MatchId
                val joinMsg = JSONObject().apply {
                    put("cid", UUID.randomUUID().toString())
                    put("match_join", JSONObject().apply {
                        if (token.isNotEmpty()) {
                            put("token", token)
                        } else if (matchId.isNotEmpty()) {
                            put("match_id", matchId)
                        }
                    })
                }
                webSocket?.send(joinMsg.toString())

                if (matchId.isNotEmpty()) {
                    activeMatchId = matchId
                }

                _matchState.value = OnlineMatchState.IN_MATCH

                scope.launch {
                    _matchEvents.emit(
                        OnlineMatchEvent.MatchFound(
                            matchId = activeMatchId ?: "match_active",
                            selfPlayerIndex = myPlayerIndex,
                            opponentName = oppName
                        )
                    )
                }
            } else if (json.has("match")) {
                val matchObj = json.getJSONObject("match")
                val realMatchId = matchObj.optString("match_id", "")
                if (realMatchId.isNotEmpty()) {
                    activeMatchId = realMatchId
                    Log.i("NakamaRepository", "Successfully joined Nakama match session: $realMatchId")
                }
            } else if (json.has("match_data")) {
                val matchData = json.getJSONObject("match_data")
                val opCode = matchData.getInt("op_code")
                val dataBase64 = matchData.optString("data", "")
                val payload = if (dataBase64.isNotEmpty()) String(Base64.decode(dataBase64, Base64.DEFAULT)) else "{}"
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
                            _matchEvents.emit(
                                OnlineMatchEvent.OpponentMove(
                                    Move.WallPlacement(Wall(r, c, isHorizontal, owner))
                                )
                            )
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
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error parsing WebSocket message: ${e.message}")
        }
    }

    fun sendOnlineMove(move: Move) {
        val matchId = activeMatchId ?: return
        val ws = webSocket ?: return

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

            val payloadBase64 = Base64.encodeToString(payload.toString().toByteArray(), Base64.NO_WRAP)
            val msg = JSONObject().apply {
                put("match_data_send", JSONObject().apply {
                    put("match_id", matchId)
                    put("op_code", opCode)
                    put("data", payloadBase64)
                })
            }

            ws.send(msg.toString())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending online move: ${e.message}")
        }
    }

    fun sendSurrender() {
        val matchId = activeMatchId ?: return
        val ws = webSocket ?: return

        try {
            val winnerIndex = if (myPlayerIndex == 0) 1 else 0
            val payload = JSONObject().apply {
                put("winnerIndex", winnerIndex)
            }
            val payloadBase64 = Base64.encodeToString(payload.toString().toByteArray(), Base64.NO_WRAP)
            val msg = JSONObject().apply {
                put("match_data_send", JSONObject().apply {
                    put("match_id", matchId)
                    put("op_code", 4)
                    put("data", payloadBase64)
                })
            }
            ws.send(msg.toString())
        } catch (e: Exception) {
            Log.e("NakamaRepository", "Error sending surrender: ${e.message}")
        }
    }

    fun cancelMatchmaking() {
        webSocket?.close(1000, "User cancelled matchmaking")
        webSocket = null
        _matchState.value = OnlineMatchState.IDLE
    }

    fun leaveMatch() {
        webSocket?.close(1000, "Left match")
        webSocket = null
        activeMatchId = null
        _matchState.value = OnlineMatchState.IDLE
    }
}
