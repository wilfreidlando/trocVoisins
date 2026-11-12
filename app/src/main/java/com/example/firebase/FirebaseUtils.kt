package com.example.firebase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CancellationException

private const val TAG_APP_CHECK = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

/**
 * Safely retrieve FirebaseAuth instance without crashing if FirebaseApp is not ready.
 */
fun getFirebaseAuthSafe(): FirebaseAuth? {
    return try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e("FirebaseUtils", "FirebaseAuth instance unavailable: ${e.message}")
        null
    }
}

/**
 * Configure Firebase App Check for debug and production variants safely.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = try { FirebaseApp.getInstance().persistenceKey } catch (e: Exception) { "default" }
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG_APP_CHECK, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG_APP_CHECK, "No debug token found in Intent extra.")
            }

            try {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG_APP_CHECK, "DebugAppCheckProviderFactory installed.")
            } catch (e: Exception) {
                Log.w(TAG_APP_CHECK, "App Check Provider installation skipped: ${e.message}")
            }
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    try {
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                        Log.d(TAG_APP_CHECK, "RecaptchaAppCheckProviderFactory installed.")
                    } catch (e: Exception) {
                        Log.w(TAG_APP_CHECK, "Recaptcha Provider installation skipped: ${e.message}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG_APP_CHECK, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * Resolves the provisioned Firestore database instance using R.string.firestore_database_id.
 */
fun getAppFirestore(context: Context): FirebaseFirestore? {
    return try {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    } catch (e: Exception) {
        Log.e("FirebaseUtils", "Firestore database instance creation failed: ${e.message}")
        try {
            FirebaseFirestore.getInstance()
        } catch (e2: Exception) {
            null
        }
    }
}

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = getFirebaseAuthSafe()
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Interactive Google Sign-In via Credential Manager using default_web_client_id.
 */
fun performGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("Configuration Google Sign-In introuvable: default_web_client_id manquant")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val auth = getFirebaseAuthSafe()
                if (auth != null) {
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } else {
                    onError("Service FirebaseAuth indisponible")
                }
            } else {
                onError("Type de justificatif inattendu")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In annulé: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Échec Google Sign-In", e)
            onError(e.localizedMessage ?: "Échec de connexion")
        }
    }
}

/**
 * Sign out cleanly from CredentialManager and FirebaseAuth.
 */
fun performSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                getFirebaseAuthSafe()?.signOut()
                onComplete()
            }
        }
    }
}
