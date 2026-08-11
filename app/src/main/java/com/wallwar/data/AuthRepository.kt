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
import kotlinx.coroutines.suspendCancellableCoroutine
import com.wallwar.data.nakama.NakamaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import com.wallwar.data.auth.GoogleAuthManager
import com.wallwar.data.auth.GoogleAuthResult

sealed class SignInResult {
    data class Success(val name: String, val email: String) : SignInResult()
    object Cancelled : SignInResult()
    data class Error(val message: String) : SignInResult()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository,
    private val settingsRepository: SettingsRepository,
    private val googleAuthManager: GoogleAuthManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_auth", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _userProfile = MutableStateFlow(loadStoredProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (nakamaRepository.hasValidSession()) {
                syncFromNakamaServer()
            } else {
                val initialProfile = _userProfile.value
                nakamaRepository.ensureAuthenticatedGuest(initialProfile.displayName)
                syncFromNakamaServer()
            }
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
                    email = "guest@wallwar.app",
                    nakamaUserId = nakamaRepository.getNakamaUserId()
                )
                saveProfile(updated)
                SignInResult.Success(updated.displayName, updated.email)
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
                email = email,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            SignInResult.Success(updated.displayName, email)
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
            val displayName = username?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@")
            nakamaRepository.authenticateWithEmail(
                email = email,
                password = password,
                create = isRegister,
                username = displayName
            )

            // Sync user data directly from Nakama Account and Storage
            syncFromNakamaServer()

            val current = _userProfile.value
            val updated = current.copy(
                isLoggedIn = true,
                displayName = if (current.displayName.contains("Guest", ignoreCase = true)) displayName else current.displayName,
                email = email,
                nakamaUserId = nakamaRepository.getNakamaUserId()
            )
            saveProfile(updated)
            SignInResult.Success(updated.displayName, email)
        } catch (e: IllegalArgumentException) {
            SignInResult.Error(e.message ?: "Authentication failed")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in authenticateWithEmail: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: e.message ?: "Email authentication error")
        }
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
            val avatarUrl = stats.optString("avatarUrl", current.photoUrl ?: "")

            val updated = current.copy(
                trophies = trophies,
                coins = coins,
                wins = wins,
                totalMatches = totalMatches,
                wallsPlaced = wallsPlaced,
                level = level,
                xp = xp,
                rankTitle = rankTitle,
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

        val email = currentUser?.email
            ?: prefs.getString("email", "guest@wallwar.app") ?: "guest@wallwar.app"
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
            coins = coins
        )
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
            .apply()
        _userProfile.value = profile

        // Sync to Nakama
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.syncUserProfileToNakama(profile)
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

    suspend fun authenticateWithGoogle(idToken: String, displayName: String? = null): SignInResult {
        return try {
            val name = displayName?.ifBlank { "Duelist" } ?: "Duelist"
            nakamaRepository.authenticateWithGoogle(idToken, name)
            syncFromNakamaServer()

            val current = _userProfile.value
            val updated = current.copy(
                isLoggedIn = true,
                displayName = name,
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
                // Authenticate with Nakama Server using extracted Google ID Token
                authenticateWithGoogle(googleResult.idToken, googleResult.displayName)
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
            email = "guest@wallwar.app",
            photoUrl = null
        )
        saveProfile(updated)
    }

    fun addCoins(amount: Int) {
        val current = _userProfile.value
        val updated = current.copy(coins = current.coins + amount)
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            nakamaRepository.rpcProcessCoinTransaction(amount, "coin_credit")
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

    fun recordArenaMatchResult(didWin: Boolean, wallsPlaced: Int, winningPrize: Int) {
        val current = _userProfile.value
        val newWins = if (didWin) current.wins + 1 else current.wins
        val newMatches = current.totalMatches + 1
        val newWalls = current.wallsPlaced + wallsPlaced
        val newXp = current.xp + if (didWin) 150 else 50
        val newTrophies = (current.trophies + if (didWin) 25 else -10).coerceAtLeast(0)
        
        // Payout winning prize if player won. Entry fee was already deducted when joining.
        val prizeAmount = if (didWin) winningPrize else 0
        val newCoins = current.coins + prizeAmount
        val newLevel = (newXp / 500) + 1

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
            coins = newCoins
        )
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (prizeAmount > 0) {
                nakamaRepository.rpcProcessCoinTransaction(prizeAmount, "arena_win_payout")
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
        }
    }

    fun recordMatchResult(didWin: Boolean, wallsPlaced: Int) {
        val current = _userProfile.value
        val newWins = if (didWin) current.wins + 1 else current.wins
        val newMatches = current.totalMatches + 1
        val newWalls = current.wallsPlaced + wallsPlaced
        val newXp = current.xp + if (didWin) 150 else 50
        val newTrophies = (current.trophies + if (didWin) 25 else -10).coerceAtLeast(0)
        val newCoins = current.coins + if (didWin) 75 else 20
        val newLevel = (newXp / 500) + 1

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
            coins = newCoins
        )
        saveProfile(updated)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
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
        }
    }
}
