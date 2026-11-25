package com.example.firebase.auth

/**
 * State representing current Firebase authentication.
 */
sealed class AuthState {
  object Unauthenticated : AuthState()
  object Loading : AuthState()
  data class Authenticated(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean = false
  ) : AuthState()
  data class Error(val message: String) : AuthState()
}

/**
 * Common user credentials representation.
 */
data class AuthUser(
  val uid: String,
  val email: String?,
  val displayName: String?,
  val isAnonymous: Boolean = false
)
