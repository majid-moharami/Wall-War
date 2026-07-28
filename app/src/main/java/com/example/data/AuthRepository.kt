package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_auth", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadStoredProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadStoredProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val name = prefs.getString("display_name", "Guest Duelist") ?: "Guest Duelist"
        val email = prefs.getString("email", "guest@wallwar.app") ?: "guest@wallwar.app"
        val photoUrl = prefs.getString("photo_url", null)
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

    suspend fun signInWithGoogle(context: Context, serverClientId: String? = null): Boolean {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId ?: "100000000000-samplegoogleclientid.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val displayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.id.substringBefore("@").replace(".", " ").capitalizeWords()
                val email = googleIdTokenCredential.id
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                signInWithGoogleAccountDetails(displayName, email, photoUrl)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Credential Manager error: ${e.message}")
            false
        }
    }

    fun signInWithGoogleAccountDetails(displayName: String, email: String, photoUrl: String? = null) {
        val current = _userProfile.value
        val updated = current.copy(
            isLoggedIn = true,
            displayName = if (displayName.isNotBlank()) displayName else "Google Duelist",
            email = email,
            photoUrl = photoUrl
        )
        saveProfile(updated)
    }

    fun signOut() {
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

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
