package com.example.firebase.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Service managing Firebase Authentication operations:
 * - Registration with email & password
 * - Login with email & password
 * - Anonymous sign-in (for frictionless companion onboarding)
 * - Sign out
 * - Current user retrieval
 * - Reactive authentication state observation Flow
 */
class FirebaseAuthService(
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

  val currentUser: AuthUser?
    get() = auth.currentUser?.toAuthUser()

  val currentUserId: String?
    get() = auth.currentUser?.uid

  /**
   * Emits reactive authentication states as Firebase Auth state changes.
   */
  fun observeAuthState(): Flow<AuthState> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      if (user != null) {
        trySend(AuthState.Authenticated(
          uid = user.uid,
          email = user.email,
          displayName = user.displayName,
          isAnonymous = user.isAnonymous
        ))
      } else {
        trySend(AuthState.Unauthenticated)
      }
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  /**
   * Registers a new user with email and password.
   */
  suspend fun registerWithEmail(email: String, pass: String): Result<AuthUser> = runCatching {
    require(email.isNotBlank()) { "Email cannot be blank" }
    require(pass.length >= 6) { "Password must be at least 6 characters" }
    val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
    val user = authResult.user ?: error("Failed to retrieve created Firebase user")
    user.toAuthUser()
  }

  /**
   * Logs in an existing user with email and password.
   */
  suspend fun loginWithEmail(email: String, pass: String): Result<AuthUser> = runCatching {
    require(email.isNotBlank()) { "Email cannot be blank" }
    require(pass.isNotBlank()) { "Password cannot be blank" }
    val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
    val user = authResult.user ?: error("Failed to sign in user")
    user.toAuthUser()
  }

  /**
   * Signs in anonymously to ensure immediate secure access to Firestore and Storage.
   */
  suspend fun signInAnonymously(): Result<AuthUser> = runCatching {
    val authResult = auth.signInAnonymously().await()
    val user = authResult.user ?: error("Failed to sign in anonymously")
    user.toAuthUser()
  }

  /**
   * Signs out the currently authenticated user.
   */
  fun signOut() {
    auth.signOut()
  }

  private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    isAnonymous = isAnonymous
  )
}
