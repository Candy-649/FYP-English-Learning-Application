package com.example.everydayenglish.data.Repository

interface AuthRepository {
    /** Firebase UID of the currently logged-in user, or null if nobody is logged in (guest mode). */
    val currentUserId: String?

    /** Email of the currently logged-in user, or null if nobody is logged in. */
    val currentUserEmail: String?

    val isLoggedIn: Boolean get() = currentUserId != null

    suspend fun register(email: String, password: String): Result<String>

    suspend fun login(email: String, password: String): Result<String>

    fun logout()
}