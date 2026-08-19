package com.wallwar.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthResult {
    data class Success(
        val idToken: String,
        val displayName: String,
        val email: String,
        val photoUrl: String? = null
    ) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
}

@Singleton
class GoogleAuthManager @Inject constructor() {

    private val tag = "GoogleAuthManager"

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    suspend fun getGoogleIdToken(callingContext: Context, customClientId: String? = null): GoogleAuthResult {
        val clientId = customClientId?.ifBlank { null } ?: AuthConstants.GOOGLE_WEB_CLIENT_ID

        if (clientId.isBlank()) {
            return GoogleAuthResult.Error("Google Web Client ID is not configured.")
        }

        val activityContext = callingContext.findActivity() ?: callingContext

        val credentialManager = try {
            CredentialManager.create(activityContext)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize CredentialManager: ${e.message}", e)
            return GoogleAuthResult.Error("Failed to initialize Google Sign-In: ${e.message}")
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = activityContext, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.givenName
                    ?: googleIdTokenCredential.id.substringBefore("@")
                val email = googleIdTokenCredential.id
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                if (idToken.isNotBlank()) {
                    Log.i(tag, "Google ID Token retrieved successfully for: $email, photoUrl=$photoUrl")
                    GoogleAuthResult.Success(idToken = idToken, displayName = displayName, email = email, photoUrl = photoUrl)
                } else {
                    GoogleAuthResult.Error("Google returned an empty ID token.")
                }
            } else {
                GoogleAuthResult.Error("Unexpected credential type returned: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(tag, "User cancelled Google Sign-In")
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(tag, "NoCredentialException: ${e.message}")
            GoogleAuthResult.Error("No Google account found on device, or SHA-1 is not configured in Google Cloud Console.")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(tag, "Failed to parse Google ID Token: ${e.message}", e)
            GoogleAuthResult.Error("Failed to parse Google ID token: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed [${e.javaClass.simpleName}]: ${e.message}", e)
            GoogleAuthResult.Error(e.localizedMessage ?: e.message ?: "Authentication failed")
        }
    }
}


