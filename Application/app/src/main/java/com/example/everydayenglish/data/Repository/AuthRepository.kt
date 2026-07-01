package com.example.everydayenglish.data.Repository

interface AuthRepository {
    val currentUserId: String?

    val currentUserEmail: String?

    val isAnonymous: Boolean

    val isLoggedIn: Boolean get() = currentUserId != null

    suspend fun continueAsGuest(): Result<String>

    suspend fun register(email: String, password: String): Result<String>

    suspend fun login(email: String, password: String): Result<String>

    fun logout()
}
