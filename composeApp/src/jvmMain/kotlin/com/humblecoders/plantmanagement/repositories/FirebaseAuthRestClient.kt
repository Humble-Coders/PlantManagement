package com.humblecoders.plantmanagement.repositories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthResponse(
    @SerialName("idToken") val idToken: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("refreshToken") val refreshToken: String = "",
    @SerialName("expiresIn") val expiresIn: String = "",
    @SerialName("localId") val localId: String = ""
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val code: Int,
    val message: String,
    val errors: List<ErrorInfo>? = null
)

@Serializable
data class ErrorInfo(
    val message: String,
    val domain: String,
    val reason: String
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)

class FirebaseAuthRestClient(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthResponse> {
        return try {
            val httpResponse = client.post(
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }

            if (httpResponse.status.isSuccess()) {
                val response: AuthResponse = httpResponse.body()
                Result.success(response)
            } else {
                val errorBody: ErrorResponse? = runCatching { httpResponse.body<ErrorResponse>() }.getOrNull()
                val message = parseErrorMessage(errorBody?.error?.message)
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            val message = parseErrorMessage(e.message)
            Result.failure(Exception(message))
        }
    }

    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<AuthResponse> {
        return try {
            val httpResponse = client.post(
                "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }

            if (httpResponse.status.isSuccess()) {
                val response: AuthResponse = httpResponse.body()
                Result.success(response)
            } else {
                val errorBody: ErrorResponse? = runCatching { httpResponse.body<ErrorResponse>() }.getOrNull()
                val message = parseErrorMessage(errorBody?.error?.message)
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            val message = parseErrorMessage(e.message)
            Result.failure(Exception(message))
        }
    }

    private fun parseErrorMessage(errorMessage: String?): String {
        return when {
            errorMessage?.contains("EMAIL_EXISTS") == true -> "Email already exists"
            errorMessage?.contains("INVALID_EMAIL") == true -> "Invalid email address"
            errorMessage?.contains("WEAK_PASSWORD") == true -> "Password is too weak"
            errorMessage?.contains("EMAIL_NOT_FOUND") == true -> "Email not found"
            errorMessage?.contains("INVALID_PASSWORD") == true -> "Invalid password"
            errorMessage?.contains("USER_DISABLED") == true -> "User account has been disabled"
            errorMessage?.contains("TOO_MANY_ATTEMPTS_TRY_LATER") == true ->
                "Too many failed attempts. Please try again later"
            else -> errorMessage ?: "Authentication failed"
        }
    }

    fun close() {
        client.close()
    }
}