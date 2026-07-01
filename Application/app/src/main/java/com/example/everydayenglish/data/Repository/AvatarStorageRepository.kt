package com.example.everydayenglish.data.Repository

import android.net.Uri

interface AvatarStorageRepository {
    suspend fun uploadAvatar(userId: String, localUri: Uri): Result<Uri>
    suspend fun uploadBackground(userId: String, localUri: Uri): Result<Uri>
}
