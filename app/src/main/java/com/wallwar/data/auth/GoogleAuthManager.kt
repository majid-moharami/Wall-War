package com.wallwar.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthResult {
    data class Success(val idToken: String, val displayName: String, val email: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
}

@Singleton
class GoogleAuthManager @Inject constructor() {

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

        if (clientId.isBlank() || clientId == "YOUR_WEB_CLIENT_ID_HERE") {
            Log.w("GoogleAuthManager", "Google Web Client ID is placeholder or blank.")
            return GoogleAuthResult.Error(
                "Google Web Client ID is not configured yet. Please set GOOGLE_WEB_CLIENT_ID in AuthConstants.kt."
            )
        }

        val activityContext = callingContext.findActivity() ?: callingContext
        val nonce = "wallwar_login_${System.currentTimeMillis()}"

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(nonce)
            .build()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(nonce)
            .build()

        val credentialManager = CredentialManager.create(activityContext)

        // Try GetSignInWithGoogleOption first, then fallback to GetGoogleIdOption or combined request
        suspend fun executeRequest(requestOptions: GetCredentialRequest): GoogleAuthResult {
            val result = credentialManager.getCredential(context = activityContext, request = requestOptions)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.id.substringBefore("@")
                val email = googleIdTokenCredential.id

                return GoogleAuthResult.Success(idToken = idToken, displayName = displayName, email = email)
            } else {
                return GoogleAuthResult.Error("Unsupported credential type returned from Credential Manager.")
            }
        }

        return try {
            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            executeRequest(primaryRequest)
        } catch (e: GetCredentialCancellationException) {
            Log.i("GoogleAuthManager", "User cancelled Google Sign-In bottom sheet")
            GoogleAuthResult.Cancelled
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "Primary GetSignInWithGoogleOption failed [${e.javaClass.simpleName}]: ${e.message}. Trying GetGoogleIdOption fallback...")
            try {
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                executeRequest(fallbackRequest)
            } catch (e2: GetCredentialCancellationException) {
                Log.i("GoogleAuthManager", "User cancelled Google Sign-In bottom sheet")
                GoogleAuthResult.Cancelled
            } catch (e2: NoCredentialException) {
                Log.w("GoogleAuthManager", "No Google account credentials found: ${e2.message}")
                GoogleAuthResult.Error(
                    "No Google account found on device, or app SHA-1 fingerprint is not configured in Google Cloud Console."
                )
            } catch (e2: GetCredentialException) {
                Log.e("GoogleAuthManager", "Credential Manager exception [${e2.type}]: ${e2.message}", e2)
                GoogleAuthResult.Error("Google Sign-In failed: ${e2.message}")
            } catch (e2: GoogleIdTokenParsingException) {
                Log.e("GoogleAuthManager", "Invalid Google ID Token: ${e2.message}", e2)
                GoogleAuthResult.Error("Failed to parse Google ID token.")
            } catch (e2: Exception) {
                Log.e("GoogleAuthManager", "Authentication error: ${e2.message}", e2)
                GoogleAuthResult.Error("Google Sign-In failed: ${e2.localizedMessage ?: e2.message}")
            }
        }
    }
}
