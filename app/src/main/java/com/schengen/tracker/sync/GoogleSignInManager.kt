@file:Suppress("DEPRECATION")

package com.schengen.tracker.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps Google Sign-In with the Drive AppData scope.
 *
 * Use [signInIntent] to launch the sign-in UI from an Activity; once
 * the result returns, call [handleSignInResult] with the data Intent.
 *
 * The classic Google Sign-In SDK is being deprecated in favor of Credential Manager,
 * but it remains the simplest way to obtain a Drive AppData OAuth token today.
 */
class GoogleSignInManager(context: Context) {

    private val appContext = context.applicationContext

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    private val client: GoogleSignInClient = GoogleSignIn.getClient(appContext, signInOptions)

    private val _account = MutableStateFlow(GoogleSignIn.getLastSignedInAccount(appContext))
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    fun signInIntent(): Intent = client.signInIntent

    fun handleSignInResult(data: Intent?): SignInResult {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            if (account == null) {
                SignInResult.Failure(
                    statusCode = CommonStatusCodes.ERROR,
                    message = "Google returned no account."
                )
            } else {
                _account.value = account
                SignInResult.Success(account)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in failed: code=${e.statusCode} message=${e.message}", e)
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> SignInResult.Cancelled
                else -> SignInResult.Failure(
                    statusCode = e.statusCode,
                    message = e.localizedMessage
                        ?: GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in threw unexpected error", e)
            SignInResult.Failure(
                statusCode = CommonStatusCodes.ERROR,
                message = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        client.signOut().addOnCompleteListener {
            _account.value = null
            onComplete()
        }
    }

    fun hasDriveScope(account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    fun refresh() {
        _account.value = GoogleSignIn.getLastSignedInAccount(appContext)
    }

    companion object {
        private const val TAG = "GoogleSignInManager"
    }
}

sealed interface SignInResult {
    data class Success(val account: GoogleSignInAccount) : SignInResult
    data object Cancelled : SignInResult
    data class Failure(val statusCode: Int, val message: String) : SignInResult
}
