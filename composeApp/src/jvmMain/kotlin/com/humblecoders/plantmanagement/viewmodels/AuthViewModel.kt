package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.User
import com.humblecoders.plantmanagement.repositories.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) {
    var authState by mutableStateOf(AuthState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        // Check if user is already logged in
        checkCurrentUser()
    }

    /**
     * Check if there's a current authenticated user
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            authState = authState.copy(isLoading = true)

            val user = authRepository.getCurrentUser()
            authState = if (user != null) {
                authState.copy(
                    isLoading = false,
                    currentUser = user,
                    isAuthenticated = true,
                    error = null
                )
            } else {
                authState.copy(
                    isLoading = false,
                    currentUser = null,
                    isAuthenticated = false,
                    error = null
                )
            }
        }
    }

    /**
     * Sign up with email and password
     */
    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            authState = authState.copy(error = "Email and password cannot be empty")
            return
        }

        if (password.length < 6) {
            authState = authState.copy(error = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            authState = authState.copy(isLoading = true, error = null)

            val result = authRepository.signUp(email, password)

            authState = if (result.isSuccess) {
                authState.copy(
                    isLoading = false,
                    currentUser = result.getOrNull(),
                    isAuthenticated = true,
                    error = null
                )
            } else {
                authState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }

    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            authState = authState.copy(error = "Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            authState = authState.copy(isLoading = true, error = null)

            val result = authRepository.signIn(email, password)

            authState = if (result.isSuccess) {
                authState.copy(
                    isLoading = false,
                    currentUser = result.getOrNull(),
                    isAuthenticated = true,
                    error = null
                )
            } else {
                authState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign in failed"
                )
            }
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        authRepository.signOut()
        authState = AuthState(
            isLoading = false,
            currentUser = null,
            isAuthenticated = false,
            error = null
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        authState = authState.copy(error = null)
    }
}