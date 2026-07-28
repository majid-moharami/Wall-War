package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
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
import kotlinx.coroutines.suspendCancellableCoroutine
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
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_auth", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _userProfile = MutableStateFlow(loadStoredProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadStoredProfile(): UserProfile {
        val currentUser = try {
            firebaseAuth.currentUser
        } catch (e: Exception) {
            null
        }

        val isLoggedIn = currentUser != null || prefs.getBoolean("is_logged_in", false)
        val name = currentUser?.displayName
            ?: prefs.getString("display_name", "Guest Duelist") ?: "Guest Duelist"
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
            wallsPlaced = wallsPlaced
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
            .apply()
        _userProfile.value = profile
    }

    private fun getWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val client = context.getString(resId)
            if (client.isNotBlank()) return client
        }
        return context.getString(R.string.default_web_client_id)
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
    }

    suspend fun signInWithGoogle(context: Context, serverClientId: String? = null): SignInResult {
        val clientId = serverClientId ?: getWebClientId(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Validate and authenticate ID Token with Firebase Auth
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).awaitTask()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val displayName = firebaseUser.displayName
                        ?: googleIdTokenCredential.displayName
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "Google User"
                    val email = firebaseUser.email ?: googleIdTokenCredential.id
                    val photoUrl = firebaseUser.photoUrl?.toString()
                        ?: googleIdTokenCredential.profilePictureUri?.toString()

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
                    SignInResult.Error("Firebase Auth verification failed: No user returned")
                }
            } else {
                SignInResult.Error("Unsupported credential type received from Credential Manager")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i("AuthRepository", "User cancelled Google Sign-In")
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w("AuthRepository", "No Google accounts available on device: ${e.message}")
            SignInResult.Error("No Google account found on this device. Please sign in to a Google account in Android settings.")
        } catch (e: GetCredentialException) {
            Log.e("AuthRepository", "Credential Manager exception: ${e.message}", e)
            SignInResult.Error("Google Sign-In error: ${e.message}")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthRepository", "Invalid Google ID Token: ${e.message}", e)
            SignInResult.Error("Invalid Google ID token format")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Authentication error: ${e.message}", e)
            SignInResult.Error("Firebase Authentication failed: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun signOut(context: Context) {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out of Firebase: ${e.message}")
        }

        try {
            val credentialManager = CredentialManager.create(context)
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

    fun recordMatchResult(didWin: Boolean, wallsPlaced: Int) {
        val current = _userProfile.value
        val newWins = if (didWin) current.wins + 1 else current.wins
        val newMatches = current.totalMatches + 1
        val newWalls = current.wallsPlaced + wallsPlaced
        val newXp = current.xp + if (didWin) 150 else 50
        val newTrophies = (current.trophies + if (didWin) 25 else -10).coerceAtLeast(0)
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
            rankTitle = newRank
        )
        saveProfile(updated)
    }
}
