package com.wallwar.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.wallwar.R
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import com.wallwar.data.nakama.NakamaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import com.wallwar.analytics.AnalyticsManager
import com.wallwar.data.auth.GoogleAuthManager
import com.wallwar.data.auth.GoogleAuthResult

sealed class SignInResult {
    data class Success(val name: String, val email: String? = null) : SignInResult()
    object Cancelled : SignInResult()
    data class Error(val message: String) : SignInResult()
}

data class MatchResultDelta(
    val didWin: Boolean,
    val trophyDelta: Int,
    val xpGained: Int,
    val prizeCoins: Int,
    val streakBonusCoins: Int,
    val totalCoinsGained: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val currentWinStreak: Int,
    val longestWinStreak: Int
)

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository,
    private val settingsRepository: SettingsRepository,
    private val googleAuthManager: GoogleAuthManager,
    private val analyticsManager: AnalyticsManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_auth", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _userProfile = MutableStateFlow(loadStoredProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _unlockedEmojiIds = MutableStateFlow(loadUnlockedEmojis())
    val unlockedEmojiIds: StateFlow<Set<String>> = _unlockedEmojiIds.asStateFlow()

    private val _unlockedAvatarSkinIds = MutableStateFlow(loadUnlockedAvatarSkins())
    val unlockedAvatarSkinIds: StateFlow<Set<String>> = _unlockedAvatarSkinIds.asStateFlow()

    private val _unlockedBallSkinIds = MutableStateFlow(loadUnlockedBallSkins())
    val unlockedBallSkinIds: StateFlow<Set<String>> = _unlockedBallSkinIds.asStateFlow()

    private val _equippedBallSkinId = MutableStateFlow(loadEquippedBallSkin())
    val equippedBallSkinId: StateFlow<String> = _equippedBallSkinId.asStateFlow()

    private val _unlockedWallSkinIds = MutableStateFlow(loadUnlockedWallSkins())
    val unlockedWallSkinIds: StateFlow<Set<String>> = _unlockedWallSkinIds.asStateFlow()

    private val _equippedWallSkinId = MutableStateFlow(loadEquippedWallSkin())
    val equippedWallSkinId: StateFlow<String> = _equippedWallSkinId.asStateFlow()

    private val _abandonedMatchNotice = MutableStateFlow<String?>(null)
    val abandonedMatchNotice: StateFlow<String?> = _abandonedMatchNotice.asStateFlow()

    init {
        checkAndResolveAbandonedMatch()
        val initialProfile = _userProfile.value
        analyticsManager.setUserId(initialProfile.nakamaUserId ?: initialProfile.displayName)
        analyticsManager.setUserProperty("display_name", initialProfile.displayName)
        analyticsManager.setUserProperty("is_logged_in", initialProfile.isLoggedIn.toString())
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (nakamaRepository.hasValidSession()) {
                syncFromNakamaServer()
            } else {
                nakamaRepository.ensureAuthenticatedGuest(initialProfile.displayName)
                syncFromNakamaServer()
            }
        }
    }

    fun clearAbandonedMatchNotice() {
        _abandonedMatchNotice.value = null
    }

    fun markActiveOnlineMatch(matchId: String) {
        prefs.edit()
            .putString("active_online_match_id", matchId)
            .putLong("active_online_match_time", System.currentTimeMillis())
            .apply()
    }

    fun clearActiveOnlineMatch() {
        prefs.edit()
            .remove("active_online_match_id")
            .remove("active_online_match_time")
            .apply()
    }

    fun checkAndResolveAbandonedMatch() {
        val abandonedMatchId = prefs.getString("active_online_match_id", null)
        if (!abandonedMatchId.isNullOrEmpty()) {
            clearActiveOnlineMatch()
            recordArenaMatchResult(didWin = false, wallsPlaced = 0, winningPrize = 0)
            _abandonedMatchNotice.value = "An unfinished online match was detected from your previous session. A forfeit loss (-10 trophies) has been recorded."
        }
    }

    fun hasValidSavedSession(): Boolean {
        return nakamaRepository.hasValidSession()
    }

    suspend fun authenticateWithDevice(deviceId: String, username: String? = null): SignInResult {
        return try {
            val displayName = username?.ifBlank { "Guest_${deviceId.takeLast(4)}" } ?: "Guest_${deviceId.takeLast(4)}"
            val success = nakamaRepository.authenticateWithDevice(deviceId, displayName)
            if (success) {
                syncFromNakamaServer()
                val current = _userProfile.value
                val updated = current.copy(
                    isLoggedIn = true,
                    displayName = if (current.displayName.isBlank()) displayName else current.displayName,
                    email = null,
                    nakamaUserId = nakamaRepository.getNakamaUserId()
                )
                saveProfile(updated)
                SignInResult.Success(updated.displayName, null)
            } else {
                SignInResult.Error("Device authentication failed. Please check server connection.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in authenticateWithDevice: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Device authentication error")
        }
    }

    suspend fun linkGoogle(idToken: String): SignInResult {
        return try {
            nakamaRepository.linkGoogle(idToken)
            syncFromNakamaServer()
            val current = _userProfile.value
            val updated = current.copy(
                isLoggedIn = true,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            SignInResult.Success(updated.displayName, updated.email)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error linking Google account: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Failed to link Google account")
        }
    }

    suspend fun linkEmail(email: String, password: String): SignInResult {
        return try {
            nakamaRepository.linkEmail(email, password)
            syncFromNakamaServer()
            val current = _userProfile.value
            val updated = current.copy(
                isLoggedIn = true,
                email = email.trim(),
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            SignInResult.Success(updated.displayName, email.trim())
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error linking Email account: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Failed to link Email account")
        }
    }

    suspend fun authenticateWithEmail(
        email: String,
        password: String,
        isRegister: Boolean,
        username: String? = null
    ): SignInResult {
        return try {
            val cleanEmail = email.trim()
            val initialDisplayName = username?.trim()?.ifBlank { null } ?: cleanEmail.substringBefore("@")
            nakamaRepository.authenticateWithEmail(
                email = cleanEmail,
                password = password,
                create = isRegister,
                username = cleanEmail // email is username on nakama
            )

            // Sync user data directly from Nakama Account and Storage
            syncFromNakamaServer()

            val current = _userProfile.value
            val chosenDisplayName = if (current.displayName.isNotBlank() && !current.displayName.contains("Guest", ignoreCase = true)) {
                current.displayName
            } else {
                initialDisplayName
            }
            val updated = current.copy(
                isLoggedIn = true,
                displayName = chosenDisplayName,
                email = cleanEmail,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            // If display name was newly set or changed, sync it to Nakama
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                nakamaRepository.syncUserProfileToNakama(updated)
            }
            SignInResult.Success(updated.displayName, cleanEmail)
        } catch (e: IllegalArgumentException) {
            SignInResult.Error(e.message ?: "Authentication failed")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in authenticateWithEmail: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Email authentication error")
        }
    }

    private fun getEmojiPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "unlocked_emojis_set_$id"
    }

    private fun loadUnlockedEmojis(email: String? = null, userId: String? = null): Set<String> {
        val key = getEmojiPrefsKey(email, userId)
        val stored = prefs.getStringSet(key, null) ?: prefs.getStringSet("unlocked_emojis_set", null)
        val defaultSet = com.wallwar.data.EmojiSkinCatalog.DEFAULT_UNLOCKED_IDS
        return if (stored.isNullOrEmpty()) {
            defaultSet
        } else {
            stored.toSet() + defaultSet
        }
    }

    private fun saveUnlockedEmojis(emojis: Set<String>, email: String? = null, userId: String? = null) {
        val key = getEmojiPrefsKey(email, userId)
        prefs.edit()
            .putStringSet(key, emojis)
            .putStringSet("unlocked_emojis_set", emojis)
            .apply()
    }

    private fun getAvatarSkinPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "unlocked_avatar_skins_set_$id"
    }

    private fun loadUnlockedAvatarSkins(email: String? = null, userId: String? = null): Set<String> {
        val key = getAvatarSkinPrefsKey(email, userId)
        val stored = prefs.getStringSet(key, null) ?: prefs.getStringSet("unlocked_avatar_skins_set", null)
        val defaultSet = com.wallwar.data.ProfileSkinCatalog.DEFAULT_UNLOCKED_SKIN_IDS
        return if (stored.isNullOrEmpty()) {
            defaultSet
        } else {
            stored.toSet() + defaultSet
        }
    }

    private fun saveUnlockedAvatarSkins(skins: Set<String>, email: String? = null, userId: String? = null) {
        val key = getAvatarSkinPrefsKey(email, userId)
        prefs.edit()
            .putStringSet(key, skins)
            .putStringSet("unlocked_avatar_skins_set", skins)
            .apply()
    }

    private fun getBallSkinPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "unlocked_ball_skins_set_$id"
    }

    private fun getEquippedBallPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "equipped_ball_skin_id_$id"
    }

    private fun loadUnlockedBallSkins(email: String? = null, userId: String? = null): Set<String> {
        val key = getBallSkinPrefsKey(email, userId)
        val stored = prefs.getStringSet(key, null) ?: prefs.getStringSet("unlocked_ball_skins_set", null)
        val defaultSet = com.wallwar.data.BallSkinCatalog.DEFAULT_UNLOCKED_BALL_IDS
        return if (stored.isNullOrEmpty()) {
            defaultSet
        } else {
            stored.toSet() + defaultSet
        }
    }

    private fun saveUnlockedBallSkins(skins: Set<String>, email: String? = null, userId: String? = null) {
        val key = getBallSkinPrefsKey(email, userId)
        prefs.edit()
            .putStringSet(key, skins)
            .putStringSet("unlocked_ball_skins_set", skins)
            .apply()
    }

    private fun loadEquippedBallSkin(email: String? = null, userId: String? = null): String {
        val key = getEquippedBallPrefsKey(email, userId)
        return prefs.getString(key, null) ?: prefs.getString("equipped_ball_skin_id", com.wallwar.data.BallSkinCatalog.DEFAULT_EQUIPPED_BALL_ID) ?: com.wallwar.data.BallSkinCatalog.DEFAULT_EQUIPPED_BALL_ID
    }

    private fun saveEquippedBallSkin(skinId: String, email: String? = null, userId: String? = null) {
        val key = getEquippedBallPrefsKey(email, userId)
        prefs.edit()
            .putString(key, skinId)
            .putString("equipped_ball_skin_id", skinId)
            .apply()
    }

    private fun getWallSkinPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "unlocked_wall_skins_set_$id"
    }

    private fun getEquippedWallPrefsKey(email: String?, userId: String?): String {
        val sanitizedEmail = email?.takeIf { it.isNotBlank() && it != "guest@wallwar.app" }?.replace("@", "_")?.replace(".", "_")
        val sanitizedUser = userId?.takeIf { it.isNotBlank() }
        val id = sanitizedEmail ?: sanitizedUser ?: "guest"
        return "equipped_wall_skin_id_$id"
    }

    private fun loadUnlockedWallSkins(email: String? = null, userId: String? = null): Set<String> {
        val key = getWallSkinPrefsKey(email, userId)
        val stored = prefs.getStringSet(key, null) ?: prefs.getStringSet("unlocked_wall_skins_set", null)
        val defaultSet = com.wallwar.data.WallSkinCatalog.DEFAULT_UNLOCKED_WALL_IDS
        return if (stored.isNullOrEmpty()) {
            defaultSet
        } else {
            stored.toSet() + defaultSet
        }
    }

    private fun saveUnlockedWallSkins(skins: Set<String>, email: String? = null, userId: String? = null) {
        val key = getWallSkinPrefsKey(email, userId)
        prefs.edit()
            .putStringSet(key, skins)
            .putStringSet("unlocked_wall_skins_set", skins)
            .apply()
    }

    private fun loadEquippedWallSkin(email: String? = null, userId: String? = null): String {
        val key = getEquippedWallPrefsKey(email, userId)
        return prefs.getString(key, null) ?: prefs.getString("equipped_wall_skin_id", com.wallwar.data.WallSkinCatalog.DEFAULT_EQUIPPED_WALL_ID) ?: com.wallwar.data.WallSkinCatalog.DEFAULT_EQUIPPED_WALL_ID
    }

    private fun saveEquippedWallSkin(skinId: String, email: String? = null, userId: String? = null) {
        val key = getEquippedWallPrefsKey(email, userId)
        prefs.edit()
            .putString(key, skinId)
            .putString("equipped_wall_skin_id", skinId)
            .apply()
    }

    suspend fun syncFromNakamaServer() {
        val stats = nakamaRepository.fetchUserProfileFromNakama()
        if (stats != null) {
            val current = _userProfile.value
            val trophies = stats.optInt("trophies", current.trophies)
            val coins = stats.optInt("coins", current.coins)
            val wins = stats.optInt("wins", current.wins)
            val totalMatches = stats.optInt("totalMatches", current.totalMatches)
            val wallsPlaced = stats.optInt("wallsPlaced", current.wallsPlaced)
            val level = stats.optInt("level", current.level)
            val xp = stats.optInt("xp", current.xp)
            val rankTitle = stats.optString("rankTitle", current.rankTitle)
            val currentWinStreak = stats.optInt("currentWinStreak", current.currentWinStreak)
            val longestWinStreak = stats.optInt("longestWinStreak", current.longestWinStreak)
            val avatarUrl = stats.optString("avatarUrl", current.photoUrl ?: "")
            val serverDisplayName = stats.optString("displayName", "")
            val serverUsername = stats.optString("username", "")

            val resolvedDisplayName = if (serverDisplayName.isNotBlank()) {
                serverDisplayName
            } else {
                current.displayName
            }
            val resolvedEmail = if (current.email.isNullOrBlank() && serverUsername.contains("@")) {
                serverUsername
            } else {
                current.email
            }

            val updated = current.copy(
                displayName = resolvedDisplayName,
                email = resolvedEmail,
                trophies = trophies,
                coins = coins,
                wins = wins,
                totalMatches = totalMatches,
                wallsPlaced = wallsPlaced,
                level = level,
                xp = xp,
                rankTitle = rankTitle,
                currentWinStreak = currentWinStreak,
                longestWinStreak = longestWinStreak,
                photoUrl = if (avatarUrl.isBlank()) null else avatarUrl,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
        }

        // Sync settings from server
        try {
            val serverTheme = nakamaRepository.fetchUserSettingsFromNakama()
            if (serverTheme != null) {
                settingsRepository.setBoardTheme(serverTheme, syncToServer = false)
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not restore server settings: ${e.message}")
        }

        // Sync unlocked emoji skins from Nakama server for this specific account
        try {
            val currentProfile = _userProfile.value
            val serverEmojis = nakamaRepository.fetchEmojiSkinsFromNakama()
            val defaultSet = com.wallwar.data.EmojiSkinCatalog.DEFAULT_UNLOCKED_IDS
            if (!serverEmojis.isNullOrEmpty()) {
                val accountEmojis = (serverEmojis + defaultSet).toSet()
                _unlockedEmojiIds.value = accountEmojis
                saveUnlockedEmojis(accountEmojis, currentProfile.email, currentProfile.nakamaUserId)
                Log.d("AuthRepository", "Restored ${accountEmojis.size} emojis from server for ${currentProfile.displayName}")
            } else {
                // If server has no emojis recorded yet, load local account cache and push to server
                val localEmojis = loadUnlockedEmojis(currentProfile.email, currentProfile.nakamaUserId)
                _unlockedEmojiIds.value = localEmojis
                nakamaRepository.syncEmojiSkinsToNakama(localEmojis)
                Log.d("AuthRepository", "Initialized server emojis for ${currentProfile.displayName}: $localEmojis")
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not restore server emoji skins: ${e.message}")
        }

        // Sync unlocked avatar skins from Nakama server for this specific account
        try {
            val currentProfile = _userProfile.value
            val avatarResult = nakamaRepository.fetchAvatarSkinsFromNakama()
            val defaultSkinSet = com.wallwar.data.ProfileSkinCatalog.DEFAULT_UNLOCKED_SKIN_IDS
            val localSkins = loadUnlockedAvatarSkins(currentProfile.email, currentProfile.nakamaUserId)
            if (avatarResult != null) {
                val (serverSkins, selectedSkin) = avatarResult
                val accountSkins = (serverSkins + localSkins + defaultSkinSet).toSet()
                _unlockedAvatarSkinIds.value = accountSkins
                saveUnlockedAvatarSkins(accountSkins, currentProfile.email, currentProfile.nakamaUserId)

                // If local had skins not yet recorded on server, update server cloud storage
                if (accountSkins.size > serverSkins.size) {
                    nakamaRepository.syncAvatarSkinsToNakama(accountSkins, selectedSkin ?: _userProfile.value.photoUrl)
                }

                // Restore equipped avatar if available on server
                if (!selectedSkin.isNullOrBlank()) {
                    val updated = _userProfile.value.copy(photoUrl = selectedSkin)
                    saveProfile(updated)
                }
                Log.d("AuthRepository", "Restored ${accountSkins.size} avatar skins from server for ${currentProfile.displayName}")
            } else {
                _unlockedAvatarSkinIds.value = localSkins
                nakamaRepository.syncAvatarSkinsToNakama(localSkins, _userProfile.value.photoUrl)
                Log.d("AuthRepository", "Initialized server avatar skins for ${currentProfile.displayName}: $localSkins")
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not restore server avatar skins: ${e.message}")
        }

        // Sync unlocked ball skins from Nakama server for this specific account
        try {
            val currentProfile = _userProfile.value
            val ballResult = nakamaRepository.fetchBallSkinsFromNakama()
            val defaultBallSet = com.wallwar.data.BallSkinCatalog.DEFAULT_UNLOCKED_BALL_IDS
            val localBalls = loadUnlockedBallSkins(currentProfile.email, currentProfile.nakamaUserId)
            if (ballResult != null) {
                val (serverBalls, selectedBall) = ballResult
                val accountBalls = (serverBalls + localBalls + defaultBallSet).toSet()
                _unlockedBallSkinIds.value = accountBalls
                saveUnlockedBallSkins(accountBalls, currentProfile.email, currentProfile.nakamaUserId)

                // If local had skins (e.g. newly won in spinner) not yet on server, update server cloud storage
                if (accountBalls.size > serverBalls.size) {
                    nakamaRepository.syncBallSkinsToNakama(accountBalls, selectedBall ?: _equippedBallSkinId.value)
                }

                if (!selectedBall.isNullOrBlank()) {
                    _equippedBallSkinId.value = selectedBall
                    saveEquippedBallSkin(selectedBall, currentProfile.email, currentProfile.nakamaUserId)
                }
                Log.d("AuthRepository", "Restored ${accountBalls.size} ball skins from server for ${currentProfile.displayName} (selected: $selectedBall)")
            } else {
                _unlockedBallSkinIds.value = localBalls
                val equipped = loadEquippedBallSkin(currentProfile.email, currentProfile.nakamaUserId)
                _equippedBallSkinId.value = equipped
                nakamaRepository.syncBallSkinsToNakama(localBalls, equipped)
                Log.d("AuthRepository", "Initialized server ball skins for ${currentProfile.displayName}: $localBalls")
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not restore server ball skins: ${e.message}")
        }

        // Sync unlocked wall skins from Nakama server for this specific account
        try {
            val currentProfile = _userProfile.value
            val wallResult = nakamaRepository.fetchWallSkinsFromNakama()
            val defaultWallSet = com.wallwar.data.WallSkinCatalog.DEFAULT_UNLOCKED_WALL_IDS
            val localWalls = loadUnlockedWallSkins(currentProfile.email, currentProfile.nakamaUserId)
            if (wallResult != null) {
                val (serverWalls, selectedWall) = wallResult
                val accountWalls = (serverWalls + localWalls + defaultWallSet).toSet()
                _unlockedWallSkinIds.value = accountWalls
                saveUnlockedWallSkins(accountWalls, currentProfile.email, currentProfile.nakamaUserId)

                // If local had wall skins not yet on server, update server cloud storage
                if (accountWalls.size > serverWalls.size) {
                    nakamaRepository.syncWallSkinsToNakama(accountWalls, selectedWall ?: _equippedWallSkinId.value)
                }

                if (!selectedWall.isNullOrBlank()) {
                    _equippedWallSkinId.value = selectedWall
                    saveEquippedWallSkin(selectedWall, currentProfile.email, currentProfile.nakamaUserId)
                }
                Log.d("AuthRepository", "Restored ${accountWalls.size} wall skins from server for ${currentProfile.displayName} (selected: $selectedWall)")
            } else {
                _unlockedWallSkinIds.value = localWalls
                val equipped = loadEquippedWallSkin(currentProfile.email, currentProfile.nakamaUserId)
                _equippedWallSkinId.value = equipped
                nakamaRepository.syncWallSkinsToNakama(localWalls, equipped)
                Log.d("AuthRepository", "Initialized server wall skins for ${currentProfile.displayName}: $localWalls")
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not restore server wall skins: ${e.message}")
        }
    }

    fun unlockWallSkin(skinId: String, priceCoins: Int): Boolean {
        val currentSet = _unlockedWallSkinIds.value
        if (currentSet.contains(skinId)) {
            return true
        }

        val profile = _userProfile.value
        if (profile.coins < priceCoins) {
            return false
        }

        val successDeduct = deductCoins(priceCoins)
        if (!successDeduct) return false

        val newSet = currentSet + skinId
        _unlockedWallSkinIds.value = newSet
        saveUnlockedWallSkins(newSet, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncWallSkinsToNakama(newSet, _equippedWallSkinId.value)
        }
        return true
    }

    fun equipWallSkin(skinId: String) {
        val profile = _userProfile.value
        _equippedWallSkinId.value = skinId
        saveEquippedWallSkin(skinId, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncWallSkinsToNakama(_unlockedWallSkinIds.value, skinId)
        }
    }

    fun unlockBallSkin(skinId: String, priceCoins: Int): Boolean {
        val currentSet = _unlockedBallSkinIds.value
        if (currentSet.contains(skinId)) {
            return true
        }

        val profile = _userProfile.value
        if (profile.coins < priceCoins) {
            return false
        }

        val successDeduct = deductCoins(priceCoins)
        if (!successDeduct) return false

        val newSet = currentSet + skinId
        _unlockedBallSkinIds.value = newSet
        saveUnlockedBallSkins(newSet, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncBallSkinsToNakama(newSet, _equippedBallSkinId.value)
        }
        return true
    }

    fun grantRewardBallSkin(skinId: String) {
        val currentSet = _unlockedBallSkinIds.value
        if (currentSet.contains(skinId)) {
            return
        }

        val newSet = currentSet + skinId
        _unlockedBallSkinIds.value = newSet
        val profile = _userProfile.value
        saveUnlockedBallSkins(newSet, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncBallSkinsToNakama(newSet, _equippedBallSkinId.value)
        }
    }

    fun equipBallSkin(skinId: String) {
        val profile = _userProfile.value
        _equippedBallSkinId.value = skinId
        saveEquippedBallSkin(skinId, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncBallSkinsToNakama(_unlockedBallSkinIds.value, skinId)
        }
    }

    fun unlockAvatarSkin(skinId: String, priceCoins: Int): Boolean {
        val currentSet = _unlockedAvatarSkinIds.value
        if (currentSet.contains(skinId)) {
            return true
        }

        val profile = _userProfile.value
        if (profile.coins < priceCoins) {
            return false
        }

        val successDeduct = deductCoins(priceCoins)
        if (!successDeduct) return false

        val newSet = currentSet + skinId
        _unlockedAvatarSkinIds.value = newSet
        saveUnlockedAvatarSkins(newSet, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncAvatarSkinsToNakama(newSet, _userProfile.value.photoUrl)
        }
        return true
    }

    fun equipAvatarSkin(skinId: String) {
        val current = _userProfile.value
        val skinIdentifier = if (skinId.startsWith("skin:")) skinId else "skin:$skinId"
        val updated = current.copy(photoUrl = skinIdentifier)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncAvatarSkinsToNakama(_unlockedAvatarSkinIds.value, skinIdentifier)
        }
    }

    fun equipGoogleAvatar(googlePhotoUrl: String) {
        val current = _userProfile.value
        val updated = current.copy(photoUrl = googlePhotoUrl)
        saveProfile(updated)
        prefs.edit().putString("google_photo_url", googlePhotoUrl).apply()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncAvatarSkinsToNakama(_unlockedAvatarSkinIds.value, googlePhotoUrl)
        }
    }

    fun getSavedGooglePhotoUrl(): String? {
        return prefs.getString("google_photo_url", null)
    }

    fun unlockEmojiSkin(emojiId: String, priceCoins: Int): Boolean {
        val currentSet = _unlockedEmojiIds.value
        if (currentSet.contains(emojiId)) {
            return true
        }

        val profile = _userProfile.value
        if (profile.coins < priceCoins) {
            return false
        }

        val successDeduct = deductCoins(priceCoins)
        if (!successDeduct) return false

        val newSet = currentSet + emojiId
        _unlockedEmojiIds.value = newSet
        saveUnlockedEmojis(newSet, profile.email, profile.nakamaUserId)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncEmojiSkinsToNakama(newSet)
        }
        return true
    }

    fun updateDisplayName(newDisplayName: String) {
        val trimmed = newDisplayName.trim()
        if (trimmed.isBlank()) return
        val current = _userProfile.value
        val updated = current.copy(displayName = trimmed)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncUserProfileToNakama(updated)
        }
    }

    private fun loadStoredProfile(): UserProfile {
        val currentUser = try {
            firebaseAuth.currentUser
        } catch (e: Exception) {
            null
        }

        val isLoggedIn = currentUser != null || prefs.getBoolean("is_logged_in", false)
        var name = currentUser?.displayName
            ?: prefs.getString("display_name", null)

        // Generate a random guest name if needed
        if (!isLoggedIn && name == null) {
            name = "Guest_${(1000..9999).random()}"
            prefs.edit().putString("display_name", name).apply()
        } else if (name == null) {
            name = "Guest Duelist"
        }

        val storedEmail = currentUser?.email ?: prefs.getString("email", null)
        val email = if (storedEmail.isNullOrBlank() || storedEmail == "guest@wallwar.app") null else storedEmail
        val photoUrl = currentUser?.photoUrl?.toString()
            ?: prefs.getString("photo_url", null)

        val trophies = prefs.getInt("trophies", 0)
        val xp = prefs.getInt("xp", 0)
        val level = prefs.getInt("level", 1)
        val rankTitle = prefs.getString("rank_title", "Novice Duelist") ?: "Novice Duelist"
        val wins = prefs.getInt("wins", 0)
        val totalMatches = prefs.getInt("total_matches", 0)
        val wallsPlaced = prefs.getInt("walls_placed", 0)
        val coins = prefs.getInt("coins", 150)
        val currentWinStreak = prefs.getInt("current_win_streak", 0)
        val longestWinStreak = prefs.getInt("longest_win_streak", 0)

        return UserProfile(
            isLoggedIn = isLoggedIn,
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            trophies = trophies,
            xp = xp,
            level = level,
            rankTitle = rankTitle,
            wins = wins,
            totalMatches = totalMatches,
            wallsPlaced = wallsPlaced,
            coins = coins,
            currentWinStreak = currentWinStreak,
            longestWinStreak = longestWinStreak
        )
    }

    fun restoreProfileData(restoredProfile: UserProfile) {
        saveProfile(restoredProfile)
    }

    private fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putBoolean("is_logged_in", profile.isLoggedIn)
            .putString("display_name", profile.displayName)
            .putString("email", profile.email)
            .putString("photo_url", profile.photoUrl)
            .putInt("trophies", profile.trophies)
            .putInt("xp", profile.xp)
            .putInt("level", profile.level)
            .putString("rank_title", profile.rankTitle)
            .putInt("wins", profile.wins)
            .putInt("total_matches", profile.totalMatches)
            .putInt("walls_placed", profile.wallsPlaced)
            .putInt("coins", profile.coins)
            .putInt("current_win_streak", profile.currentWinStreak)
            .putInt("longest_win_streak", profile.longestWinStreak)
            .apply()
        _userProfile.value = profile

        // Sync to Nakama
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncUserProfileToNakama(profile)
        }
    }

    private fun getWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val client = context.getString(resId)
            if (client.isNotBlank()) return client
        }
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            ""
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
    }

    suspend fun authenticateWithGoogle(
        idToken: String,
        displayName: String? = null,
        photoUrl: String? = null
    ): SignInResult {
        return try {
            val name = displayName?.ifBlank { "Duelist" } ?: "Duelist"
            nakamaRepository.authenticateWithGoogle(idToken, name)
            syncFromNakamaServer()

            if (!photoUrl.isNullOrBlank()) {
                prefs.edit().putString("google_photo_url", photoUrl).apply()
            }

            val current = _userProfile.value
            // If user hasn't set a custom profile skin, automatically use the Google account photo
            val shouldApplyGooglePhoto = !photoUrl.isNullOrBlank() && 
                    (current.photoUrl.isNullOrBlank() || current.photoUrl == "skin:skin_default" || !com.wallwar.data.ProfileSkinCatalog.isSkinUrl(current.photoUrl))
            
            val updated = current.copy(
                isLoggedIn = true,
                displayName = name,
                photoUrl = if (shouldApplyGooglePhoto) photoUrl else current.photoUrl,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            SignInResult.Success(updated.displayName, updated.email)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in authenticateWithGoogle: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Google authentication error")
        }
    }

    suspend fun signInWithGoogle(callingContext: Context, serverClientId: String? = null): SignInResult {
        val googleResult = googleAuthManager.getGoogleIdToken(callingContext, serverClientId)
        return when (googleResult) {
            is GoogleAuthResult.Success -> {
                // Authenticate with Nakama Server using extracted Google ID Token & Profile Photo
                authenticateWithGoogle(googleResult.idToken, googleResult.displayName, googleResult.photoUrl)
            }
            is GoogleAuthResult.Cancelled -> SignInResult.Cancelled
            is GoogleAuthResult.Error -> SignInResult.Error(googleResult.message)
        }
    }

    suspend fun signOut(callingContext: Context) {
        val activityContext = callingContext.findActivity() ?: callingContext

        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out of Firebase: ${e.message}")
        }

        try {
            val credentialManager = CredentialManager.create(activityContext)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error clearing credential state: ${e.message}")
        }

        // Cleanly clear local Nakama session without wiping server data
        nakamaRepository.logout()

        val current = _userProfile.value
        val updated = current.copy(
            isLoggedIn = false,
            displayName = "Guest Duelist",
            email = null,
            photoUrl = null
        )
        saveProfile(updated)
        _unlockedEmojiIds.value = loadUnlockedEmojis(null, null)
        _unlockedAvatarSkinIds.value = com.wallwar.data.ProfileSkinCatalog.DEFAULT_UNLOCKED_SKIN_IDS
        _unlockedBallSkinIds.value = com.wallwar.data.BallSkinCatalog.DEFAULT_UNLOCKED_BALL_IDS
        _equippedBallSkinId.value = com.wallwar.data.BallSkinCatalog.DEFAULT_EQUIPPED_BALL_ID
    }

    fun addCoins(amount: Int) {
        val current = _userProfile.value
        val updated = current.copy(coins = current.coins + amount)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.rpcProcessCoinTransaction(amount, "coin_credit")
        }
    }

    fun processGooglePlayCoinPurchase(
        productId: String,
        amount: Int,
        purchaseToken: String,
        orderId: String
    ) {
        val current = _userProfile.value
        val updated = current.copy(coins = current.coins + amount)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val serverBalance = nakamaRepository.rpcVerifyAndProcessGooglePlayPurchase(
                productId = productId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                amountCoins = amount
            )
            if (serverBalance >= 0) {
                val latest = _userProfile.value
                if (latest.coins != serverBalance) {
                    saveProfile(latest.copy(coins = serverBalance))
                }
            }
        }
    }

    fun deductCoins(amount: Int): Boolean {
        val current = _userProfile.value
        if (current.coins < amount) return false
        val updated = current.copy(coins = current.coins - amount)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.rpcProcessCoinTransaction(-amount, "entry_fee")
        }
        return true
    }

    fun recordArenaMatchResult(
        didWin: Boolean,
        wallsPlaced: Int,
        winningPrize: Int,
        isOnline: Boolean = true
    ): MatchResultDelta {
        val current = _userProfile.value
        if (!isOnline) {
            // For AI or offline practice games, no XP, coins, trophies, or rewards are given
            return MatchResultDelta(
                didWin = didWin,
                trophyDelta = 0,
                xpGained = 0,
                prizeCoins = 0,
                streakBonusCoins = 0,
                totalCoinsGained = 0,
                oldLevel = current.level,
                newLevel = current.level,
                leveledUp = false,
                currentWinStreak = current.currentWinStreak,
                longestWinStreak = current.longestWinStreak
            )
        }

        val newWins = if (didWin) current.wins + 1 else current.wins
        val newMatches = current.totalMatches + 1
        val newWalls = current.wallsPlaced + wallsPlaced
        val xpGain = if (didWin) 150 else 50
        val newXp = current.xp + xpGain

        // Trophies and competitive streak only apply to real online matches
        val trophyDelta = if (didWin) 25 else if (current.trophies >= 10) -10 else -current.trophies
        val newTrophies = (current.trophies + trophyDelta).coerceAtLeast(0)

        val newStreak = if (didWin) current.currentWinStreak + 1 else 0
        val newLongestStreak = maxOf(current.longestWinStreak, newStreak)
        val streakBonus = if (didWin && newStreak >= 2) (newStreak * 10).coerceAtMost(100) else 0
        
        // Payout winning prize if player won + streak bonus
        val prizeAmount = if (didWin) winningPrize else 0
        val totalCoinsAdded = prizeAmount + streakBonus
        val newCoins = current.coins + totalCoinsAdded
        val oldLevel = current.level
        val newLevel = (newXp / 500) + 1
        val leveledUp = newLevel > oldLevel

        val newRank = when {
            newTrophies >= 1000 -> "Apex Cybermaster"
            newTrophies >= 500 -> "Neon Grandmaster"
            newTrophies >= 200 -> "Neon Knight"
            else -> "Novice Duelist"
        }

        val updated = current.copy(
            wins = newWins,
            totalMatches = newMatches,
            wallsPlaced = newWalls,
            xp = newXp,
            trophies = newTrophies,
            level = newLevel,
            rankTitle = newRank,
            coins = newCoins,
            currentWinStreak = newStreak,
            longestWinStreak = newLongestStreak
        )
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (totalCoinsAdded > 0) {
                nakamaRepository.rpcProcessCoinTransaction(totalCoinsAdded, "arena_win_payout")
            }
            nakamaRepository.recordMatchHistoryToNakama(
                MatchRecord(
                    modeName = "Arena Match",
                    opponentName = "Opponent",
                    winnerPlayer = if (didWin) 0 else 1,
                    totalMoves = 10,
                    totalWallsPlaced = wallsPlaced,
                    durationSeconds = 60L
                )
            )
            nakamaRepository.syncUserProfileToNakama(updated)
        }

        return MatchResultDelta(
            didWin = didWin,
            trophyDelta = trophyDelta,
            xpGained = xpGain,
            prizeCoins = prizeAmount,
            streakBonusCoins = streakBonus,
            totalCoinsGained = totalCoinsAdded,
            oldLevel = oldLevel,
            newLevel = newLevel,
            leveledUp = leveledUp,
            currentWinStreak = newStreak,
            longestWinStreak = newLongestStreak
        )
    }

    fun recordMatchResult(didWin: Boolean, wallsPlaced: Int): MatchResultDelta {
        val current = _userProfile.value
        val newWins = if (didWin) current.wins + 1 else current.wins
        val newMatches = current.totalMatches + 1
        val newWalls = current.wallsPlaced + wallsPlaced
        val xpGain = if (didWin) 150 else 50
        val newXp = current.xp + xpGain
        val trophyDelta = if (didWin) 25 else if (current.trophies >= 10) -10 else -current.trophies
        val newTrophies = (current.trophies + trophyDelta).coerceAtLeast(0)

        val newStreak = if (didWin) current.currentWinStreak + 1 else 0
        val newLongestStreak = maxOf(current.longestWinStreak, newStreak)
        val streakBonus = if (didWin && newStreak >= 2) (newStreak * 10).coerceAtMost(100) else 0

        val baseCoins = if (didWin) 75 else 20
        val totalCoinsAdded = baseCoins + streakBonus
        val newCoins = current.coins + totalCoinsAdded
        val oldLevel = current.level
        val newLevel = (newXp / 500) + 1
        val leveledUp = newLevel > oldLevel

        val newRank = when {
            newTrophies >= 1000 -> "Apex Cybermaster"
            newTrophies >= 500 -> "Neon Grandmaster"
            newTrophies >= 200 -> "Neon Knight"
            else -> "Novice Duelist"
        }

        val updated = current.copy(
            wins = newWins,
            totalMatches = newMatches,
            wallsPlaced = newWalls,
            xp = newXp,
            trophies = newTrophies,
            level = newLevel,
            rankTitle = newRank,
            coins = newCoins,
            currentWinStreak = newStreak,
            longestWinStreak = newLongestStreak
        )
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (totalCoinsAdded > 0) {
                nakamaRepository.rpcProcessCoinTransaction(totalCoinsAdded, "match_reward")
            }
            nakamaRepository.recordMatchHistoryToNakama(
                MatchRecord(
                    modeName = "Tactical Match",
                    opponentName = "Opponent",
                    winnerPlayer = if (didWin) 0 else 1,
                    totalMoves = 10,
                    totalWallsPlaced = wallsPlaced,
                    durationSeconds = 60L
                )
            )
            nakamaRepository.syncUserProfileToNakama(updated)
        }

        return MatchResultDelta(
            didWin = didWin,
            trophyDelta = trophyDelta,
            xpGained = xpGain,
            prizeCoins = baseCoins,
            streakBonusCoins = streakBonus,
            totalCoinsGained = totalCoinsAdded,
            oldLevel = oldLevel,
            newLevel = newLevel,
            leveledUp = leveledUp,
            currentWinStreak = newStreak,
            longestWinStreak = newLongestStreak
        )
    }

    suspend fun setUserDevLevelAndCoins(targetLevel: Int = 30, targetCoins: Int = 2000000) {
        val current = _userProfile.value
        val targetXp = maxOf(current.xp, (targetLevel - 1) * 500 + 250)
        val targetRank = when {
            targetLevel >= 25 -> "Apex Cybermaster"
            targetLevel >= 15 -> "Neon Grandmaster"
            targetLevel >= 5 -> "Neon Knight"
            else -> current.rankTitle
        }

        val updated = current.copy(
            level = targetLevel,
            xp = targetXp,
            coins = targetCoins,
            rankTitle = targetRank
        )
        saveProfile(updated)

        // Sync directly to Nakama server
        try {
            nakamaRepository.syncUserProfileToNakama(updated)
            val coinDiff = targetCoins - current.coins
            if (coinDiff != 0) {
                nakamaRepository.rpcProcessCoinTransaction(coinDiff, "dev_admin_boost")
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Dev boost server sync notice: ${e.message}")
        }
    }
}
