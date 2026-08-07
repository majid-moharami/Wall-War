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
import kotlinx.coroutines.suspendCancellableCoroutine
import com.wallwar.data.nakama.NakamaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class SignInResult {
    data class Success(val name: String, val email: String) : SignInResult()
    object Cancelled : SignInResult()
    data class Error(val message: String) : SignInResult()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository,
    private val settingsRepository: SettingsRepository
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
            val initialProfile = _userProfile.value
            nakamaRepository.ensureAuthenticatedGuest(initialProfile.displayName)
            syncFromNakamaServer()
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

    suspend fun signInWithGoogle(callingContext: Context, serverClientId: String? = null): SignInResult {
        val clientId = serverClientId ?: getWebClientId(callingContext)
        if (clientId.isBlank()) {
            Log.w("AuthRepository", "Google Server Web Client ID is missing. Falling back to Guest Mode.")
            // Automatically ensure guest authentication if Google ID is missing
            val success = nakamaRepository.ensureAuthenticatedGuest(_userProfile.value.displayName)
            return if (success) {
                SignInResult.Error("Google Sign-In is not configured yet. You are playing as a Guest. Add your Client ID to strings.xml to link your Google account.")
            } else {
                SignInResult.Error("Guest authentication failed. Please check your server connection.")
            }
        }

        val activityContext = callingContext.findActivity() ?: callingContext

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce("wallwar_login_${System.currentTimeMillis()}")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activityContext)

        return try {
            val result = credentialManager.getCredential(context = activityContext, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                var displayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.id.substringBefore("@")
                var email = googleIdTokenCredential.id
                var photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                // Validate ID Token with Firebase Auth if available
                try {
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(authCredential).awaitTask()
                    val firebaseUser = authResult.user

                    if (firebaseUser != null) {
                        displayName = firebaseUser.displayName ?: displayName
                        email = firebaseUser.email ?: email
                        photoUrl = firebaseUser.photoUrl?.toString() ?: photoUrl
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Firebase Auth token exchange skipped or failed: ${e.message}")
                }

                // Authenticate with Nakama Server
                try {
                    nakamaRepository.authenticateWithGoogle(idToken, displayName)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Nakama Google authentication skipped: ${e.message}")
                }

                val current = _userProfile.value
                val updated = current.copy(
                    isLoggedIn = true,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl
                )
                saveProfile(updated)
                SignInResult.Success(displayName, email)
            } else {
                SignInResult.Error("Unsupported credential type returned from Credential Manager.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i("AuthRepository", "User cancelled Google Sign-In bottom sheet")
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w("AuthRepository", "No credential found or available: ${e.message}")
            val helpMsg = "No Google account found or app not registered. \n\n" +
                         "1. Ensure you have a Google account on this device.\n" +
                         "2. Check if your SHA-1 fingerprint is added to Google Cloud Console for package: ${context.packageName}"
            SignInResult.Error(helpMsg)
        } catch (e: GetCredentialException) {
            Log.e("AuthRepository", "Credential Manager exception [${e.type}]: ${e.message}", e)
            SignInResult.Error("Google Sign-In failed: ${e.message} (Type: ${e.type})")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthRepository", "Invalid Google ID Token: ${e.message}", e)
            SignInResult.Error("Failed to parse Google ID token.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Authentication error: ${e.message}", e)
            SignInResult.Error("Sign in failed: ${e.localizedMessage ?: e.message}")
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
