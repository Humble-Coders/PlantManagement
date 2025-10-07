package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.humblecoders.plantmanagement.data.User
import com.humblecoders.plantmanagement.data.UserRole
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: Firestore,
    private val restClient: FirebaseAuthRestClient
) {

    /**
     * Sign up a new user with email and password
     * Uses REST API for authentication and Admin SDK for user management
     */
    suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            // Sign up via REST API
            val authResult = restClient.signUpWithEmailAndPassword(email, password)

            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign up failed"))
            }

            val authResponse = authResult.getOrNull() ?: return Result.failure(Exception("Sign up failed"))
            val uid = authResponse.localId

            // Create user document in Firestore with default USER role
            val userData = mapOf(
                "email" to email,
                "role" to "user",
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .get(10, TimeUnit.SECONDS)

            val user = User(
                uid = uid,
                email = email,
                role = UserRole.USER
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     * Uses REST API for password verification
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Sign in via REST API (this verifies the password)
            val authResult = restClient.signInWithEmailAndPassword(email, password)

            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign in failed"))
            }

            val authResponse = authResult.getOrNull() ?: return Result.failure(Exception("Sign in failed"))
            val uid = authResponse.localId

            // Fetch user role from Firestore
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .get(10, TimeUnit.SECONDS)

            if (!userDoc.exists()) {
                return Result.failure(Exception("User data not found"))
            }

            val roleString = userDoc.getString("role") ?: "user"
            val role = when (roleString.lowercase()) {
                "admin" -> UserRole.ADMIN
                else -> UserRole.USER
            }

            val user = User(
                uid = uid,
                email = email,
                role = role
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        // Session management handled in ViewModel
    }

    /**
     * Get current authenticated user for this client session.
     * On desktop with Admin SDK there is no client-side session, so return null.
     */
    suspend fun getCurrentUser(): User? {
        return null
    }

    /**
     * Get user by UID (for checking existing sessions)
     */
    suspend fun getUserByUid(uid: String): User? {
        return try {
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .get(10, TimeUnit.SECONDS)

            if (!userDoc.exists()) return null

            val email = userDoc.getString("email") ?: return null
            val roleString = userDoc.getString("role") ?: "user"
            val role = when (roleString.lowercase()) {
                "admin" -> UserRole.ADMIN
                else -> UserRole.USER
            }

            User(
                uid = uid,
                email = email,
                role = role
            )
        } catch (e: Exception) {
            null
        }
    }
}